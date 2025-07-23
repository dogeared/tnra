package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.function.Consumer;

public class StatCard extends Div {
    
    private final String label;
    private final String emoji;
    private IntegerField valueField;
    private Button minusBtn;
    private Button plusBtn;
    private boolean readOnly = false;
    private Consumer<Integer> valueChangeListener;
    private boolean isUpdatingFromButton = false;
    private Integer currentValue;
    
    public StatCard(String label, String emoji, Integer initialValue) {
        this.label = label;
        this.emoji = emoji;
        this.currentValue = initialValue;
        
        addClassNames(
            LumoUtility.Background.CONTRAST_5,
            LumoUtility.BorderRadius.MEDIUM,
            LumoUtility.Padding.SMALL,
            "stat-card"
        );

        // Card header with emoji and label
        Div header = createHeader();
        
        // Value field and controls
        HorizontalLayout controls = createControls(initialValue);
        
        add(header, controls);
    }
    
    private Div createHeader() {
        Div header = new Div();
        header.addClassName("stat-card-header");
        
        Span emojiSpan = new Span(emoji);
        emojiSpan.addClassName("stat-card-emoji");
        
        Span labelSpan = new Span(label);
        labelSpan.addClassNames(LumoUtility.FontWeight.MEDIUM, LumoUtility.FontSize.SMALL, "stat-card-label");
        
        header.add(emojiSpan, labelSpan);
        return header;
    }
    
    private HorizontalLayout createControls(Integer initialValue) {
        HorizontalLayout controls = new HorizontalLayout();
        controls.setSpacing(false);
        controls.setAlignItems(FlexComponent.Alignment.CENTER);
        controls.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        // Value field - start empty for null values
        valueField = new IntegerField();
        valueField.setValue(initialValue); // null values will show as empty
        valueField.setMin(0);
        valueField.setMax(99);
        valueField.setWidth("50px");
        valueField.setLabel(null);
        valueField.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, "stat-input", "centered-input");

        // Control buttons
        minusBtn = createControlButton(VaadinIcon.MINUS, "Decrease " + label);
        plusBtn = createControlButton(VaadinIcon.PLUS, "Increase " + label);

        minusBtn.addClickListener(e -> {
            if (!readOnly) {
                if (currentValue == null) {
                    // If empty, minus button does nothing
                    return;
                } else if (currentValue > 0) {
                    currentValue--;
                    isUpdatingFromButton = true;
                    valueField.setValue(currentValue);
                    if (valueChangeListener != null) {
                        valueChangeListener.accept(currentValue);
                    }
                    addPulseAnimation(valueField);
                    isUpdatingFromButton = false;
                } else if (currentValue == 0) {
                    // If at 0, set to empty
                    currentValue = null;
                    isUpdatingFromButton = true;
                    valueField.setValue(null);
                    if (valueChangeListener != null) {
                        valueChangeListener.accept(null);
                    }
                    addPulseAnimation(valueField);
                    isUpdatingFromButton = false;
                }
            }
        });

        plusBtn.addClickListener(e -> {
            if (!readOnly) {
                if (currentValue == null) {
                    // If empty, advance to 0
                    currentValue = 0;
                    isUpdatingFromButton = true;
                    valueField.setValue(0);
                    if (valueChangeListener != null) {
                        valueChangeListener.accept(0);
                    }
                    addPulseAnimation(valueField);
                    isUpdatingFromButton = false;
                } else if (currentValue < 99) {
                    currentValue++;
                    isUpdatingFromButton = true;
                    valueField.setValue(currentValue);
                    if (valueChangeListener != null) {
                        valueChangeListener.accept(currentValue);
                    }
                    addPulseAnimation(valueField);
                    isUpdatingFromButton = false;
                }
            }
        });

        // Handle direct input
        valueField.addValueChangeListener(e -> {
            // Skip if this change was triggered by a button click
            if (isUpdatingFromButton) {
                return;
            }
            
            Integer value = e.getValue();
            if (!readOnly) {
                if (value == null) {
                    // Allow empty values
                    currentValue = null;
                    if (valueChangeListener != null) {
                        valueChangeListener.accept(null);
                    }
                } else if (value < 0) {
                    // Allow negative input to set value to null (unset)
                    valueField.setValue(null);
                    currentValue = null;
                    if (valueChangeListener != null) {
                        valueChangeListener.accept(null);
                    }
                } else if (value > 99) {
                    valueField.setValue(99);
                    value = 99;
                    currentValue = value;
                    if (valueChangeListener != null) {
                        valueChangeListener.accept(value);
                    }
                } else {
                    currentValue = value;
                    if (valueChangeListener != null) {
                        valueChangeListener.accept(value);
                    }
                }
            }
        });

        controls.add(minusBtn, valueField, plusBtn);
        return controls;
    }
    
    private Button createControlButton(VaadinIcon icon, String ariaLabel) {
        Button button = new Button(icon.create());
        button.addClassNames(
            LumoUtility.BorderRadius.MEDIUM,
            LumoUtility.Padding.SMALL,
            "control-button"
        );
        button.setAriaLabel(ariaLabel);

        // Click animation
        button.addClickListener(e -> {
            button.getStyle().set("transform", "scale(0.95)");
            button.getElement().executeJs("setTimeout(() => $0.style.transform = 'scale(1)', 100)");
        });

        return button;
    }
    
    private void addPulseAnimation(com.vaadin.flow.component.Component element) {
        element.addClassName("pulse-animation");
        element.getElement().executeJs("setTimeout(() => $0.classList.remove('pulse-animation'), 300)");
    }
    
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        valueField.setReadOnly(readOnly);
        minusBtn.setEnabled(!readOnly);
        plusBtn.setEnabled(!readOnly);
        
        // Add visual styling for read-only state
        if (readOnly) {
            valueField.addClassName("readonly-input");
            minusBtn.addClassName("readonly-button");
            plusBtn.addClassName("readonly-button");
            addClassName("readonly-card");
        } else {
            valueField.removeClassName("readonly-input");
            minusBtn.removeClassName("readonly-button");
            plusBtn.removeClassName("readonly-button");
            removeClassName("readonly-card");
        }
    }
    
    public boolean isReadOnly() {
        return readOnly;
    }
    
    public void setValue(Integer value) {
        this.currentValue = value;  // Update internal state
        valueField.setValue(value); // null values will show as empty
    }
    
    public Integer getValue() {
        return valueField.getValue();
    }
    
    public void setValueChangeListener(Consumer<Integer> listener) {
        this.valueChangeListener = listener;
    }
    
    public String getLabel() {
        return label;
    }
} 