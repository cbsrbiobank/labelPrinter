package edu.ualberta.med.biobank.labelprinter.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.gui.common.dialogs.BgcBaseDialog;
import edu.ualberta.med.biobank.gui.common.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;

/**
 * 
 * Creates a message dialog for obtaining alphanumeric text. Null is returned if
 * the user does not enter any text or presses the cancel button.
 * 
 * @author Thomas Polasek 2011
 * 
 */
public class StringInputDialog extends BgcBaseDialog {

    private static final I18n i18n = I18nFactory
        .getI18n(StringInputDialog.class);
    private static final String MSG_NO_ST_NAME = i18n.trc("Error Message", //$NON-NLS-1$
        "A value is required"); //$NON-NLS-1$

    private class ValuePojo {
        public String name;

        public ValuePojo() {

        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    };

    private ValuePojo value;

    private String title;

    private String message;

    private String labelText;

    public StringInputDialog(String title, String message, String labelText,
        Shell parent) {
        super(parent);
        this.title = title;
        this.message = message;
        this.labelText = labelText;
        value = new ValuePojo();
    }

    @Override
    protected String getDialogShellTitle() {
        return title;
    }

    @Override
    protected String getTitleAreaMessage() {
        return message;
    }

    @Override
    protected String getTitleAreaTitle() {
        return title;
    }

    @Override
    protected void createDialogAreaInternal(Composite parent) throws Exception {
        Composite content = new Composite(parent, SWT.NONE);
        content.setLayout(new GridLayout(2, false));
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createBoundWidgetWithLabel(content, BgcBaseText.class, SWT.BORDER,
            labelText, null, value, i18n.tr("name"), //$NON-NLS-1$
            new NonEmptyStringValidator(
                MSG_NO_ST_NAME));

        setOkButtonEnabled(false);
    }

    public String getValue() {
        return value.getName();
    }

    public void setValue(String value) {
        this.value.setName(value);
    }
}