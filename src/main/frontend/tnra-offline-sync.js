(() => {
  const STORAGE_KEY = "tnra:posts:offline-draft:v1";
  const SYNC_INTERVAL_MS = 15000;
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
    if (!window.location.pathname.startsWith("/posts")) {
      return;
    }
    const draft = readDraftFromUi();
    if (!hasDraftContent(draft)) {
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

  async function fetchInProgressPost() {
    const response = await fetch("/api/v1/in_progress", {
      method: "GET",
      credentials: "same-origin",
      headers: { Accept: "application/json" }
    });
    if (!response.ok) {
      throw new Error(`Unable to read in-progress post (${response.status})`);
    }
    return response.json();
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
    if (syncing || !navigator.onLine || !window.location.pathname.startsWith("/posts")) {
      return;
    }
    const stored = getStoredDraft();
    if (!stored) {
      return;
    }

    syncing = true;
    try {
      const serverPost = await fetchInProgressPost();
      if (!serverPost || serverPost.state !== "IN_PROGRESS") {
        return;
      }
      const merged = mergeDraft(serverPost, stored.draft);
      await pushPost(merged);
      localStorage.removeItem(STORAGE_KEY);
      window.dispatchEvent(new CustomEvent("tnra-offline-sync-success"));
    } catch (err) {
      // Keep draft; next reconnect or timer run retries.
    } finally {
      syncing = false;
    }
  }

  function listenForFieldChanges() {
    document.addEventListener("change", saveDraft, true);
    document.addEventListener("input", saveDraft, true);
  }

  function initSyncLoop() {
    window.addEventListener("online", () => {
      syncDraft();
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

  function init() {
    if (initialized) {
      return;
    }
    if (!window.location.pathname.startsWith("/posts")) {
      return;
    }
    initialized = true;
    listenForFieldChanges();
    initSyncLoop();
    syncDraft();
  }

  window.TnraOfflineSync = {
    init,
    syncDraft
  };

  window.addEventListener("beforeunload", () => {
    saveDraft();
    if (syncTimerId) {
      window.clearInterval(syncTimerId);
    }
  });
})();
