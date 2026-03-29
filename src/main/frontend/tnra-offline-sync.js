(() => {
  const STORAGE_KEY = "tnra:posts:offline-draft:v1";
  const ACTIONS_KEY = "tnra:posts:offline-actions:v1";
  const SYNC_INTERVAL_MS = 15000;
  const RESTORE_RETRY_DELAY_MS = 250;
  const RESTORE_MAX_ATTEMPTS = 8;
  const STAT_NAMES = ["exercise", "meditate", "pray", "read", "gtg", "meetings", "sponsor"];
  const FIELD_CONFIG = [
    { selector: "vaadin-text-area[label=\"What I Don't Want You To Know\"]", path: ["intro", "widwytk"] },
    { selector: "vaadin-text-field[label=\"Kryptonite\"]", path: ["intro", "kryptonite"] },
    { selector: "vaadin-text-area[label=\"What and When\"]", path: ["intro", "whatAndWhen"] },
    { selector: "vaadin-text-area[label=\"Best\"]", path: ["personal", "best"], index: 0 },
    { selector: "vaadin-text-area[label=\"Worst\"]", path: ["personal", "worst"], index: 0 },
    { selector: "vaadin-text-area[label=\"Best\"]", path: ["family", "best"], index: 1 },
    { selector: "vaadin-text-area[label=\"Worst\"]", path: ["family", "worst"], index: 1 },
    { selector: "vaadin-text-area[label=\"Best\"]", path: ["work", "best"], index: 2 },
    { selector: "vaadin-text-area[label=\"Worst\"]", path: ["work", "worst"], index: 2 }
  ];

  let initialized = false;
  let syncing = false;
  let restoring = false;
  let syncTimerId = null;

  function getElement(config) {
    const matches = document.querySelectorAll(config.selector);
    if (!matches.length) {
      return null;
    }
    return typeof config.index === "number" ? matches[config.index] : matches[0];
  }

  function setByPath(target, path, value) {
    let node = target;
    for (let i = 0; i < path.length - 1; i += 1) {
      const key = path[i];
      if (!node[key] || typeof node[key] !== "object") {
        node[key] = {};
      }
      node = node[key];
    }
    node[path[path.length - 1]] = value;
  }

  function getFieldValue(element) {
    if (!element) {
      return null;
    }
    return typeof element.value === "string" ? element.value : element.value ?? null;
  }

  function getByPath(source, path) {
    let node = source;
    for (let i = 0; i < path.length; i += 1) {
      if (node == null || typeof node !== "object") {
        return undefined;
      }
      node = node[path[i]];
    }
    return node;
  }

  function setFieldValue(element, value) {
    if (!element) {
      return false;
    }
    const nextValue = value == null ? "" : `${value}`;
    if (`${element.value ?? ""}` === nextValue) {
      return false;
    }
    element.value = nextValue;
    element.dispatchEvent(new Event("input", { bubbles: true, composed: true }));
    element.dispatchEvent(new Event("change", { bubbles: true, composed: true }));
    return true;
  }

  function readDraftFromUi() {
    const draft = {
      intro: {},
      personal: {},
      family: {},
      work: {},
      stats: {}
    };

    FIELD_CONFIG.forEach((config) => {
      const element = getElement(config);
      const value = getFieldValue(element);
      setByPath(draft, config.path, value);
    });

    const statInputs = document.querySelectorAll(".stat-input");
    STAT_NAMES.forEach((statName, index) => {
      const value = statInputs[index] ? statInputs[index].value : "";
      draft.stats[statName] = value === "" ? null : Number(value);
    });

    return draft;
  }

  function hasDraftContent(draft) {
    const values = [
      draft.intro?.widwytk,
      draft.intro?.kryptonite,
      draft.intro?.whatAndWhen,
      draft.personal?.best,
      draft.personal?.worst,
      draft.family?.best,
      draft.family?.worst,
      draft.work?.best,
      draft.work?.worst,
      draft.stats?.exercise,
      draft.stats?.meditate,
      draft.stats?.pray,
      draft.stats?.read,
      draft.stats?.gtg,
      draft.stats?.meetings,
      draft.stats?.sponsor
    ];
    return values.some((value) => value !== null && value !== undefined && `${value}`.trim() !== "");
  }

  function saveDraft() {
    if (restoring || !window.location.pathname.startsWith("/posts")) {
      return;
    }
    const draft = readDraftFromUi();
    if (!hasDraftContent(draft)) {
      localStorage.removeItem(STORAGE_KEY);
      return;
    }
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        draft,
        updatedAt: Date.now()
      })
    );
  }

  function getStoredDraft() {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      const parsed = JSON.parse(raw);
      if (!parsed || !parsed.draft) {
        return null;
      }
      return parsed;
    } catch (err) {
      return null;
    }
  }

  function getStoredActions() {
    const raw = localStorage.getItem(ACTIONS_KEY);
    if (!raw) {
      return {};
    }
    try {
      const parsed = JSON.parse(raw);
      return parsed && typeof parsed === "object" ? parsed : {};
    } catch (err) {
      return {};
    }
  }

  function saveActions(actions) {
    if (!actions || Object.keys(actions).length === 0) {
      localStorage.removeItem(ACTIONS_KEY);
      return;
    }
    localStorage.setItem(ACTIONS_KEY, JSON.stringify(actions));
  }

  async function fetchInProgressPost() {
    const response = await fetch("/api/v1/in_progress", {
      method: "GET",
      credentials: "same-origin",
      headers: { Accept: "application/json" }
    });
    if (response.status === 404) {
      return null;
    }
    if (!response.ok) {
      throw new Error(`Unable to read in-progress post (${response.status})`);
    }
    return response.json();
  }

  async function startInProgressPost() {
    const response = await fetch("/api/v1/start_from_app", {
      method: "GET",
      credentials: "same-origin",
      headers: { Accept: "application/json" }
    });
    if (!response.ok) {
      throw new Error(`Unable to start post (${response.status})`);
    }
    return response.json();
  }

  async function finishInProgressPost() {
    const response = await fetch("/api/v1/finish_from_app", {
      method: "POST",
      credentials: "same-origin",
      headers: { Accept: "application/json" }
    });
    if (!response.ok) {
      throw new Error(`Unable to finish post (${response.status})`);
    }
  }

  function mergeDraft(post, draft) {
    const merged = JSON.parse(JSON.stringify(post));
    merged.intro = { ...(merged.intro || {}), ...(draft.intro || {}) };
    merged.personal = { ...(merged.personal || {}), ...(draft.personal || {}) };
    merged.family = { ...(merged.family || {}), ...(draft.family || {}) };
    merged.work = { ...(merged.work || {}), ...(draft.work || {}) };
    merged.stats = { ...(merged.stats || {}), ...(draft.stats || {}) };
    return merged;
  }

  async function pushPost(post) {
    const response = await fetch("/api/v1/in_progress", {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json"
      },
      body: JSON.stringify(post)
    });

    if (!response.ok) {
      throw new Error(`Unable to sync post (${response.status})`);
    }
  }

  async function syncDraft() {
    if (syncing || !navigator.onLine) {
      return;
    }
    const stored = getStoredDraft();
    const actions = getStoredActions();
    if (!stored && !actions.finishAfterSync) {
      return;
    }

    syncing = true;
    try {
      let serverPost = await fetchInProgressPost();
      if (stored && (!serverPost || serverPost.state !== "IN_PROGRESS")) {
        serverPost = await startInProgressPost();
      }

      if (stored) {
        const merged = mergeDraft(serverPost, stored.draft);
        await pushPost(merged);
        localStorage.removeItem(STORAGE_KEY);
        window.dispatchEvent(new CustomEvent("tnra-offline-sync-success"));
      }

      if (actions.finishAfterSync) {
        if (!serverPost || serverPost.state !== "IN_PROGRESS") {
          serverPost = await fetchInProgressPost();
        }
        if (!serverPost || serverPost.state !== "IN_PROGRESS") {
          return;
        }
        await finishInProgressPost();
        saveActions({});
        window.dispatchEvent(new CustomEvent("tnra-offline-finish-success"));
      }
    } catch (err) {
      // Keep draft; next reconnect or timer run retries.
    } finally {
      syncing = false;
    }
  }

  function applyStoredDraftToUi(storedDraft) {
    if (!storedDraft || !storedDraft.draft) {
      return false;
    }

    restoring = true;
    let updated = false;
    try {
      FIELD_CONFIG.forEach((config) => {
        const element = getElement(config);
        const value = getByPath(storedDraft.draft, config.path);
        if (value !== undefined && value !== null) {
          updated = setFieldValue(element, value) || updated;
        }
      });

      const statInputs = document.querySelectorAll(".stat-input");
      STAT_NAMES.forEach((statName, index) => {
        const element = statInputs[index];
        const value = storedDraft.draft.stats?.[statName];
        if (value !== undefined && value !== null) {
          updated = setFieldValue(element, value) || updated;
        }
      });
    } finally {
      restoring = false;
    }

    if (updated) {
      window.dispatchEvent(new CustomEvent("tnra-offline-draft-restored"));
    }
    return updated;
  }

  function restoreDraftFromStorage(attempt = 0) {
    const stored = getStoredDraft();
    if (!stored) {
      return;
    }

    const uiDraft = readDraftFromUi();
    if (hasDraftContent(uiDraft)) {
      return;
    }

    const restored = applyStoredDraftToUi(stored);
    if (restored || attempt >= RESTORE_MAX_ATTEMPTS) {
      return;
    }

    window.setTimeout(() => {
      restoreDraftFromStorage(attempt + 1);
    }, RESTORE_RETRY_DELAY_MS);
  }

  function listenForFieldChanges() {
    document.addEventListener("change", saveDraft, true);
    document.addEventListener("input", saveDraft, true);
  }

  function initSyncLoop() {
    window.addEventListener("online", () => {
      syncDraft();
    });
    window.addEventListener("focus", () => {
      if (navigator.onLine) {
        syncDraft();
      }
    });
    document.addEventListener("visibilitychange", () => {
      if (document.visibilityState === "visible") {
        syncDraft();
      }
    });
    syncTimerId = window.setInterval(() => {
      if (navigator.onLine) {
        syncDraft();
      }
    }, SYNC_INTERVAL_MS);
  }

  function queueFinishAfterReconnect() {
    const nextActions = { ...getStoredActions(), finishAfterSync: true };
    saveActions(nextActions);
    saveDraft();
    window.dispatchEvent(new CustomEvent("tnra-offline-finish-queued"));
  }

  function wireFinishButtonOfflineQueue() {
    document.addEventListener("click", (event) => {
      const path = typeof event.composedPath === "function" ? event.composedPath() : [];
      const startButton = path.find(
        (node) => node instanceof HTMLElement && node.classList?.contains("start-new-post-button")
      );
      if (startButton && !navigator.onLine) {
        event.preventDefault();
        event.stopImmediatePropagation();
        window.dispatchEvent(new CustomEvent("tnra-offline-start-redirected"));
        window.location.assign("/offline.html");
        return;
      }

      const finishButton = path.find(
        (node) => node instanceof HTMLElement && node.classList?.contains("finish-post-button")
      );
      if (!finishButton) {
        return;
      }
      if (navigator.onLine) {
        return;
      }

      event.preventDefault();
      event.stopImmediatePropagation();
      queueFinishAfterReconnect();
    }, true);
  }

  function init() {
    if (initialized) {
      return;
    }
    initialized = true;
    initSyncLoop();
    if (window.location.pathname.startsWith("/posts")) {
      listenForFieldChanges();
      wireFinishButtonOfflineQueue();
      restoreDraftFromStorage();
    }
    syncDraft();
  }

  window.TnraOfflineSync = {
    init,
    syncDraft,
    getStoredDraft,
    getStoredActions,
    saveActions
  };

  window.addEventListener("beforeunload", () => {
    saveDraft();
    if (syncTimerId) {
      window.clearInterval(syncTimerId);
    }
  });
})();
