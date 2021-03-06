package edu.ualberta.med.biobank.labelprinter.forms;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.labelPrinter.GetSourceSpecimenUniqueInventoryIdSetAction;
import edu.ualberta.med.biobank.gui.common.BgcLogger;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.gui.common.forms.BgcEntryForm;
import edu.ualberta.med.biobank.gui.common.forms.BgcEntryFormActions;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import edu.ualberta.med.biobank.labelprinter.BarcodeGenPlugin;
import edu.ualberta.med.biobank.labelprinter.preferences.PreferenceConstants;
import edu.ualberta.med.biobank.labelprinter.preferences.PreferenceInitializer;
import edu.ualberta.med.biobank.labelprinter.progress.PrintOperation;
import edu.ualberta.med.biobank.labelprinter.progress.SaveOperation;
import edu.ualberta.med.biobank.labelprinter.template.Template;
import edu.ualberta.med.biobank.labelprinter.template.TemplateStore;
import edu.ualberta.med.biobank.labelprinter.template.presets.cbsr.CBSRData;
import edu.ualberta.med.biobank.labelprinter.template.presets.cbsr.exceptions.CBSRGuiVerificationException;
import edu.ualberta.med.biobank.server.applicationservice.exceptions.BiobankServerException;
import gov.nih.nci.system.applicationservice.ApplicationException;

/**
 * 
 * View for entering patient information, selecting a logo and picking a template file. The user
 * prints and saves the barcode label prints from this interface.
 * 
 * @author Thomas Polasek 2011
 * 
 */
public class PatientLabelEntryForm extends BgcEntryForm {

    public static final String ID = "edu.ualberta.med.biobank.labelprinter.forms.SpecimanLabelEntryForm"; //$NON-NLS-1$

    public static final I18n i18n = I18nFactory
        .getI18n(PatientLabelEntryForm.class);

    public static final BgcLogger logger = BgcLogger
        .getLogger(PatientLabelEntryForm.class.getName());

    private BgcBaseText projectTitleText = null;
    private BgcBaseText logoText = null;
    private BgcBaseText printerText = null;
    private Button logoButton = null;

    private Combo templateCombo = null;
    private Combo printerCombo = null;

    private Button barcode2DTextCheckbox = null;
    private BgcBaseText patientNumText = null;
    private BgcBaseText labelCustomTextField = null;
    private BgcBaseText labelCustomTextValue = null;
    private Button labelCustomFieldTypeCheckbox = null;
    private Button labelCustomValueTypeCheckbox = null;

    private Button customField1Checkbox = null;
    private Button customField2Checkbox = null;
    private Button customField3Checkbox = null;

    private BgcBaseText customField1Text = null;
    private BgcBaseText customField2Text = null;
    private BgcBaseText customField3Text = null;

    private Button customValue1Checkbox = null;
    private Button customValue2Checkbox = null;
    private Button customValue3Checkbox = null;

    private BgcBaseText customValue1Text = null;
    private BgcBaseText customValue2Text = null;
    private BgcBaseText customValue3Text = null;

    private Button printBarcode1Checkbox = null;
    private Button printBarcode2Checkbox = null;
    private Button printBarcode3Checkbox = null;

    private Button savePdfButton = null;

    private Shell shell;

    private IPreferenceStore perferenceStore;

    private Template loadedTemplate;
    private TemplateStore templateStore;

    @SuppressWarnings("nls")
    @Override
    protected void init() throws Exception {
        setPartName(i18n.tr("Patient Labels"));
    }

    @Override
    protected Image getFormImage() {
        return null;
    }

    @Override
    public void setFocus() {
        // do nothing for now
    }

    @Override
    public void setValues() throws Exception {
        clearFields();
    }

    @SuppressWarnings("nls")
    private void clearFieldsConfirm() {
        if (BgcPlugin
            .openConfirm(
                i18n.tr("Reset Form Information"),
                i18n.tr("Do you want to clear any information that you have entered into this form?"))) {
            clearFields();
        }
    }

    @SuppressWarnings("nls")
    private void clearFields() {
        patientNumText.setText("");
        labelCustomTextValue.setText("");

        customValue1Text.setText("");
        customValue2Text.setText("");
        customValue3Text.setText("");
    }

    @SuppressWarnings("nls")
    @Override
    public boolean print() {
        PrintOperation printOperation = null;
        BarcodeViewGuiData guiData = null;
        try {
            guiData = new BarcodeViewGuiData();

            List<String> specimenInventoryIds = SessionManager.getAppService().doAction(
                new GetSourceSpecimenUniqueInventoryIdSetAction(32)).getList();

            // print operation
            printOperation = new PrintOperation(guiData, specimenInventoryIds);

            try {
                new ProgressMonitorDialog(shell)
                    .run(true, true, printOperation);
            } catch (InvocationTargetException e1) {
                printOperation.saveFailed();
                printOperation.setError(
                    i18n.tr("Error"),
                    i18n.tr("InvocationTargetException: ")
                        + e1.getCause().getMessage());
            }

            if (printOperation.isSuccessful()) {
                updateSavePreferences();
                clearFieldsConfirm();
                return true;
            }

            if (printOperation.errorExists()) {
                BgcPlugin.openAsyncError(printOperation.getError()[0],
                    printOperation.getError()[1]);
                return false;
            }

        } catch (BiobankServerException e) {
            BgcPlugin.openAsyncError(
                i18n.tr("Specimen ID Error"),
                e.getMessage());
        } catch (ApplicationException e) {
            BgcPlugin
                .openAsyncError(
                    i18n.tr("Server Error"),
                    e.getMessage());
        } catch (CBSRGuiVerificationException e1) {
            BgcPlugin.openAsyncError(
                i18n.tr("Gui Validation"),
                e1.getMessage());
        } catch (InterruptedException e2) {
            // do nothing
        }
        return false;
    }

    @Override
    protected void addToolbarButtons() {
        formActions = new BgcEntryFormActions(this);
        addResetAction();
        addPrintAction();
        form.updateToolBar();
    }

    @SuppressWarnings("nls")
    @Override
    protected void createFormContent() throws Exception {
        super.createFormContent();
        form.setText(i18n.tr("Patient Labels"));
        form.setMessage(i18n.tr("Print source specimen labels for a patient"),
            IMessageProvider.NONE);
        page.setLayout(new GridLayout(1, false));
        loadPreferenceStore();
        shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        createTopSection();
        createPerLabelInfo();
        createPerSheetInfo();
        createActionButtonsGroup();
        templateStore = null;
        updateForm();

    }

    @SuppressWarnings("nls")
    private void updateForm() {
        try {
            if (templateStore == null) {
                templateStore = new TemplateStore();
            }
            setEnable(true);

            // remove and reload template combo
            templateCombo.removeAll();
            for (String templateName : templateStore.getTemplateNames()) {
                templateCombo.add(templateName);
            }

            if (templateCombo.getItemCount() > 0)
                templateCombo.select(0);

            for (int i = 0; i < templateCombo.getItemCount(); i++) {
                if (templateCombo.getItem(i).equals(
                    perferenceStore
                        .getString(PreferenceConstants.TEMPLATE_NAME))) {
                    templateCombo.select(i);
                    break;
                }
            }
            templateCombo.redraw();

            loadSelectedTemplate();

        } catch (ApplicationException e) {
            BgcPlugin.openAsyncError(i18n.tr("Database Error"),
                i18n.tr("Error while updating form"), e);
        }
    }

    private void setEnable(boolean enable) {
        projectTitleText.setEnabled(enable);
        logoText.setEnabled(enable);
        logoButton.setEnabled(enable);
        customField1Text.setEnabled(enable);
        customValue1Text.setEnabled(enable);
        patientNumText.setEnabled(enable);
        customField2Text.setEnabled(enable);
        customValue2Text.setEnabled(enable);
        customField3Text.setEnabled(enable);
        customValue3Text.setEnabled(enable);
        labelCustomTextField.setEnabled(enable);
        templateCombo.setEnabled(enable);
        printerCombo.setEnabled(enable);
        savePdfButton.setEnabled(enable);
    }

    @SuppressWarnings("nls")
    private void loadPreferenceStore() {
        perferenceStore = null;

        if (BarcodeGenPlugin.getDefault() != null)
            perferenceStore = BarcodeGenPlugin.getDefault()
                .getPreferenceStore();

        if (perferenceStore == null) {
            logger.error(i18n.tr("WARNING: preference store was NULL!"));
            perferenceStore = new PreferenceStore("barcodegen.properties");
            PreferenceInitializer.setDefaults(perferenceStore);
        }
    }

    @SuppressWarnings("nls")
    private void createTopSectionItems(Composite group3) {
        GridData gridData1 = new GridData();
        gridData1.horizontalAlignment = GridData.FILL;
        gridData1.grabExcessHorizontalSpace = true;
        gridData1.verticalAlignment = GridData.CENTER;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        gridLayout.makeColumnsEqualWidth = false;
        Composite composite3 = toolkit.createComposite(group3, SWT.NONE);
        composite3.setLayout(gridLayout);
        composite3.setLayoutData(gridData);
        new Label(composite3, SWT.NONE)
            .setText(i18n.tr("Title:"));

        projectTitleText = new BgcBaseText(composite3, SWT.BORDER);
        projectTitleText.setLayoutData(gridData);
        projectTitleText.setTextLimit(12);
        projectTitleText.setText(perferenceStore
            .getString(PreferenceConstants.PROJECT_TITLE));

        new Label(composite3, SWT.NONE);
        new Label(composite3, SWT.NONE)
            .setText(i18n.tr("Logo:"));
        logoText = new BgcBaseText(composite3, SWT.BORDER);
        logoText.setEditable(false);
        logoText.setLayoutData(gridData1);
        logoText.setText(perferenceStore
            .getString(PreferenceConstants.LOGO_FILE_LOCATION));
        logoButton = new Button(composite3, SWT.NONE);
        logoButton.setText(i18n.tr("Browse..."));
        logoButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent event) {

                FileDialog fd = new FileDialog(shell, SWT.OPEN);
                fd.setText(i18n.tr("Select Logo"));
                String[] filterExt = { "*.png" };
                fd.setFilterExtensions(filterExt);
                String selected = fd.open();
                if (selected != null) {
                    logoText.setText(selected);
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });

        GridData gridData21 = new GridData();
        gridData21.grabExcessHorizontalSpace = true;
        gridData21.verticalAlignment = GridData.CENTER;
        gridData21.horizontalAlignment = GridData.FILL;

        new Label(composite3, SWT.NONE)
            .setText(i18n.tr("Template:"));
        templateCombo = new Combo(composite3, SWT.DROP_DOWN | SWT.BORDER
            | SWT.READ_ONLY);
        templateCombo.setLayoutData(gridData21);
        templateCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                loadSelectedTemplate();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        new Label(composite3, SWT.NONE);
        printerText = this.createReadOnlyLabelledField(composite3, SWT.NONE,
            i18n.tr("Intended Printer"));

        new Label(composite3, SWT.NONE);
        new Label(composite3, SWT.NONE)
            .setText(i18n.tr("Printer:"));

        printerCombo = new Combo(composite3, SWT.DROP_DOWN | SWT.BORDER
            | SWT.READ_ONLY);
        printerCombo.setLayoutData(gridData21);

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null,
            null);

        for (PrintService ps : services) {
            printerCombo.add(ps.getName());
        }
        if (printerCombo.getItemCount() > 0)
            printerCombo.select(0);

        for (int i = 0; i < printerCombo.getItemCount(); i++) {
            if (printerCombo.getItem(i).equals(
                perferenceStore.getString(PreferenceConstants.PRINTER_NAME))) {
                printerCombo.select(i);
                break;
            }
        }
        loadSelectedTemplate();

    }

    /**
     * Loads the Template from templateCombo.
     * 
     * A new template is loaded for each selection change in templateCombo.
     */
    @SuppressWarnings("nls")
    private void loadSelectedTemplate() {
        if (templateCombo.getSelectionIndex() >= 0) {
            try {

                String comboSelectedTemplate = templateCombo
                    .getItem(templateCombo.getSelectionIndex());

                // already loaded
                if ((loadedTemplate == null)
                    || !loadedTemplate.getName().equals(comboSelectedTemplate)) {
                    loadedTemplate = templateStore
                        .getTemplate(comboSelectedTemplate);
                }

            } catch (Exception ee) {
                BgcPlugin.openAsyncError(
                    i18n.tr("Verification Issue"),
                    i18n.tr(
                        "Could not load template: {0}",
                        ee.getMessage()));
            }

            if (loadedTemplate != null)
                printerText.setText(loadedTemplate.getPrinterName());
        }

    }

    @SuppressWarnings("nls")
    private void createPerSheetInfo() {

        Composite group1 = createSectionWithClient(
            i18n.tr("Sheet Information", page));

        group1.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
            false));
        group1.setLayout(new GridLayout());

        createPerSheetItems(group1);
    }

    @SuppressWarnings("nls")
    private void createCustomField1(Composite composite5) {
        new Label(composite5, SWT.NONE)
            .setText(i18n.tr("Custom Field 1:"));

        customField1Checkbox = new Button(composite5, SWT.CHECK);
        customField1Checkbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!customField1Checkbox.getSelection()) {
                    customField1Text.setText("");
                    customField1Checkbox.setEnabled(false);
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        customField1Text = new BgcBaseText(composite5, SWT.BORDER);
        customField1Text.setLayoutData(new GridData(GridData.FILL,
            GridData.FILL, true, false));
        customField1Text.setTextLimit(12);
        customField1Text.setText(perferenceStore
            .getString(PreferenceConstants.LABEL_TEXT_1));

        customField1Checkbox.setSelection((customField1Text.getText() != null)
            && (customField1Text.getText().length() > 0));
        customField1Checkbox.setEnabled((customField1Text.getText() != null)
            && (customField1Text.getText().length() > 0));

        customField1Text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if ((customField1Text.getText() != null)
                    && (customField1Text.getText().length() > 0)) {
                    customField1Checkbox.setEnabled(true);
                    customField1Checkbox.setSelection(true);
                } else {
                    customField1Checkbox.setEnabled(false);
                    customField1Checkbox.setSelection(false);
                }
            }

        });

        printBarcode1Checkbox = new Button(composite5, SWT.CHECK);
        printBarcode1Checkbox.setLayoutData(new GridData(GridData.CENTER,
            GridData.CENTER, false, false));
        printBarcode1Checkbox.setSelection(perferenceStore
            .getBoolean(PreferenceConstants.BARCODE_CHECKBOX_1));
        printBarcode1Checkbox.setEnabled(false);

        customValue1Checkbox = new Button(composite5, SWT.CHECK);
        customValue1Checkbox.setSelection(false);
        customValue1Checkbox.setEnabled(false);
        customValue1Checkbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                if (!customValue1Checkbox.getSelection()) {
                    customValue1Text.setEnabled(false);
                    customValue1Checkbox.setSelection(false);
                    customValue1Checkbox.setEnabled(false);
                    customValue1Text.setText("");
                    customValue1Text.setEnabled(true);
                    printBarcode1Checkbox.setEnabled(false);

                } else {
                    printBarcode1Checkbox.setEnabled(true);

                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        customValue1Text = new BgcBaseText(composite5, SWT.BORDER);
        customValue1Text.setLayoutData(new GridData(GridData.FILL,
            GridData.FILL, true, false));
        customValue1Text.setTextLimit(24);
        customValue1Text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if ((customValue1Text.getText() != null)
                    && (customValue1Text.getText().length() > 0)) {
                    printBarcode1Checkbox.setEnabled(true);
                    customValue1Checkbox.setSelection(true);
                    customValue1Checkbox.setEnabled(true);

                } else {
                    printBarcode1Checkbox.setEnabled(false);
                    customValue1Checkbox.setSelection(false);
                    customValue1Checkbox.setEnabled(false);
                }
            }

        });

    }

    @SuppressWarnings("nls")
    private void createCustomField2(Composite composite5) {

        new Label(composite5, SWT.NONE)
            .setText(i18n.tr("Custom Field 2:"));
        customField2Checkbox = new Button(composite5, SWT.CHECK);
        customField2Checkbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!customField2Checkbox.getSelection()) {
                    customField2Text.setText("");
                    customField2Checkbox.setEnabled(false);
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        customField2Text = new BgcBaseText(composite5, SWT.BORDER);
        customField2Text.setLayoutData(new GridData(GridData.FILL,
            GridData.CENTER, false, false));
        customField2Text.setTextLimit(12);
        customField2Text.setText(perferenceStore
            .getString(PreferenceConstants.LABEL_TEXT_2));

        customField2Checkbox.setSelection((customField2Text.getText() != null)
            && (customField2Text.getText().length() > 0));
        customField2Checkbox.setEnabled((customField2Text.getText() != null)
            && (customField2Text.getText().length() > 0));

        customField2Text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if ((customField2Text.getText() != null)
                    && (customField2Text.getText().length() > 0)) {
                    customField2Checkbox.setEnabled(true);
                    customField2Checkbox.setSelection(true);
                } else {
                    customField2Checkbox.setEnabled(false);
                    customField2Checkbox.setSelection(false);
                }
            }

        });

        printBarcode2Checkbox = new Button(composite5, SWT.CHECK);
        printBarcode2Checkbox.setSelection(perferenceStore
            .getBoolean(PreferenceConstants.BARCODE_CHECKBOX_2));
        printBarcode2Checkbox.setLayoutData(new GridData(GridData.CENTER,
            GridData.CENTER, false, false));
        printBarcode2Checkbox.setEnabled(false);

        customValue2Checkbox = new Button(composite5, SWT.CHECK);
        customValue2Checkbox.setSelection(false);
        customValue2Checkbox.setEnabled(false);
        customValue2Checkbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                if (!customValue2Checkbox.getSelection()) {
                    customValue2Text.setEnabled(false);
                    customValue2Checkbox.setSelection(false);
                    customValue2Checkbox.setEnabled(false);
                    customValue2Text.setText("");
                    customValue2Text.setEnabled(true);
                    printBarcode2Checkbox.setEnabled(false);

                } else {
                    printBarcode2Checkbox.setEnabled(true);

                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        customValue2Text = new BgcBaseText(composite5, SWT.BORDER);
        customValue2Text.setLayoutData(new GridData(GridData.FILL,
            GridData.FILL, true, false));
        customValue2Text.setTextLimit(24);
        customValue2Text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if ((customValue2Text.getText() != null)
                    && (customValue2Text.getText().length() > 0)) {
                    printBarcode2Checkbox.setEnabled(true);
                    customValue2Checkbox.setSelection(true);
                    customValue2Checkbox.setEnabled(true);

                } else {
                    printBarcode2Checkbox.setEnabled(false);
                    customValue2Checkbox.setSelection(false);
                    customValue2Checkbox.setEnabled(false);
                }
            }

        });

    }

    @SuppressWarnings("nls")
    private void createCustomField3(Composite composite5) {
        new Label(composite5, SWT.NONE)
            .setText(i18n.tr("Custom Field 3:"));
        customField3Checkbox = new Button(composite5, SWT.CHECK);
        customField3Checkbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!customField3Checkbox.getSelection()) {
                    customField3Text.setText("");
                    customField3Checkbox.setEnabled(false);
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        customField3Text = new BgcBaseText(composite5, SWT.BORDER);
        customField3Text.setLayoutData(new GridData(GridData.FILL,
            GridData.CENTER, false, false));
        customField3Text.setTextLimit(12);
        customField3Text.setText(perferenceStore
            .getString(PreferenceConstants.LABEL_TEXT_3));

        customField3Checkbox.setSelection((customField3Text.getText() != null)
            && (customField3Text.getText().length() > 0));
        customField3Checkbox.setEnabled((customField3Text.getText() != null)
            && (customField3Text.getText().length() > 0));

        customField3Text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if ((customField3Text.getText() != null)
                    && (customField3Text.getText().length() > 0)) {
                    customField3Checkbox.setEnabled(true);
                    customField3Checkbox.setSelection(true);
                } else {
                    customField3Checkbox.setEnabled(false);
                    customField3Checkbox.setSelection(false);
                }
            }

        });

        printBarcode3Checkbox = new Button(composite5, SWT.CHECK);
        printBarcode3Checkbox.setSelection(perferenceStore
            .getBoolean(PreferenceConstants.BARCODE_CHECKBOX_3));
        printBarcode3Checkbox.setLayoutData(new GridData(GridData.CENTER,
            GridData.CENTER, false, false));
        printBarcode3Checkbox.setEnabled(false);

        customValue3Checkbox = new Button(composite5, SWT.CHECK);
        customValue3Checkbox.setSelection(false);
        customValue3Checkbox.setEnabled(false);
        customValue3Checkbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                if (!customValue3Checkbox.getSelection()) {
                    customValue3Text.setEnabled(false);
                    customValue3Checkbox.setSelection(false);
                    customValue3Checkbox.setEnabled(false);
                    customValue3Text.setText("");
                    customValue3Text.setEnabled(true);
                    printBarcode3Checkbox.setEnabled(false);

                } else {
                    printBarcode3Checkbox.setEnabled(true);

                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        customValue3Text = new BgcBaseText(composite5, SWT.BORDER);
        customValue3Text.setLayoutData(new GridData(GridData.FILL,
            GridData.FILL, true, false));
        customValue3Text.setTextLimit(24);
        customValue3Text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if ((customValue3Text.getText() != null)
                    && (customValue3Text.getText().length() > 0)) {
                    printBarcode3Checkbox.setEnabled(true);
                    customValue3Checkbox.setSelection(true);
                    customValue3Checkbox.setEnabled(true);

                } else {
                    printBarcode3Checkbox.setEnabled(false);
                    customValue3Checkbox.setSelection(false);
                    customValue3Checkbox.setEnabled(false);
                }
            }

        });
    }

    @SuppressWarnings("nls")
    private void createPerSheetItems(Composite group1) {
        GridLayout gridLayout2 = new GridLayout();
        gridLayout2.numColumns = 6;
        gridLayout2.makeColumnsEqualWidth = false;
        Composite composite5 = toolkit.createComposite(group1, SWT.NONE);
        composite5.setLayout(gridLayout2);
        composite5.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
            true, false));
        new Label(composite5, SWT.NONE)
            .setText(i18n.tr("Fields"));
        new Label(composite5, SWT.NONE)
            .setText(i18n.tr("Enable"));
        new Label(composite5, SWT.NONE)
            .setText(i18n.tr("Field Name"));
        new Label(composite5, SWT.NONE)
            .setText(i18n.tr("Print Barcode"));
        new Label(composite5, SWT.NONE)
            .setText(i18n.tr("Enable"));
        new Label(composite5, SWT.NONE)
            .setText(i18n.tr("Field Value"));

        createCustomField1(composite5);
        createCustomField2(composite5);
        createCustomField3(composite5);

    }

    @SuppressWarnings("nls")
    private void createPatientNumberText(Composite group1) {

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = false;
        gridData.verticalAlignment = GridData.CENTER;
        gridData.widthHint = 150;

        new Label(group1, SWT.NONE)
            .setText(i18n.tr("Patient Number:"));
        patientNumText = new BgcBaseText(group1, SWT.BORDER);
        patientNumText.setTextLimit(14);
        patientNumText.setLayoutData(gridData);
    }

    @SuppressWarnings("nls")
    private void createPerLabelInfo() {

        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.verticalAlignment = GridData.CENTER;
        gridData.widthHint = 150;

        GridLayout gridLayout5 = new GridLayout();
        gridLayout5.numColumns = 2;
        gridLayout5.makeColumnsEqualWidth = false;

        Composite group2 = createSectionWithClient(
            i18n.tr("Per Label Information"), page);
        group2.setLayoutData(gridData);
        group2.setLayout(gridLayout5);

        createPatientNumberText(group2);
        createCustomFieldText(group2);
        createBarcode2DTextCheckbox(group2);

    }

    @SuppressWarnings("nls")
    private void createBarcode2DTextCheckbox(Composite group2) {
        new Label(group2, SWT.NONE)
            .setText(i18n.tr("Barcode 2D Text:"));
        barcode2DTextCheckbox = new Button(group2, SWT.CHECK | SWT.LEFT);
        barcode2DTextCheckbox.setSelection(perferenceStore
            .getBoolean(PreferenceConstants.BARCODE_2D_TEXT_TYPE_CHECKBOX));

    }

    @SuppressWarnings("nls")
    private void createCustomFieldText(Composite group2) {

        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = false;
        gridData.verticalAlignment = GridData.FILL;

        GridLayout gridLayout3 = new GridLayout();
        gridLayout3.verticalSpacing = 1;
        gridLayout3.numColumns = 4;
        gridLayout3.horizontalSpacing = 10;
        gridLayout3.marginWidth = 0;
        gridLayout3.makeColumnsEqualWidth = false;

        new Label(group2, SWT.NONE)
            .setText(i18n.tr("Custom Field:"));

        Composite composite6 = toolkit.createComposite(group2, SWT.NONE);
        composite6.setLayout(gridLayout3);
        composite6.setLayoutData(gridData);

        labelCustomFieldTypeCheckbox = new Button(composite6, SWT.CHECK
            | SWT.LEFT);
        labelCustomFieldTypeCheckbox
            .addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (!labelCustomFieldTypeCheckbox.getSelection()) {
                        labelCustomTextField.setText("");

                        labelCustomTextValue.setText("");
                        labelCustomTextValue.setEnabled(false);
                        labelCustomValueTypeCheckbox.setSelection(false);
                        labelCustomValueTypeCheckbox.setEnabled(false);
                    }

                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    widgetSelected(e);
                }
            });

        labelCustomTextField = new BgcBaseText(composite6, SWT.BORDER);
        labelCustomTextField.setText(perferenceStore
            .getString(PreferenceConstants.SPECIMEN_TYPE_TEXT));
        labelCustomTextField.setLayoutData(gridData);
        labelCustomTextField.setTextLimit(25);
        labelCustomTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if ((labelCustomTextField.getText() != null)
                    && (labelCustomTextField.getText().length() > 0)) {
                    labelCustomFieldTypeCheckbox.setSelection(true);
                    labelCustomFieldTypeCheckbox.setEnabled(true);

                    labelCustomTextValue.setEnabled(true);

                } else {
                    labelCustomFieldTypeCheckbox.setSelection(false);
                    labelCustomFieldTypeCheckbox.setEnabled(false);

                    labelCustomTextValue.setText("");
                    labelCustomTextValue.setEnabled(false);
                    labelCustomValueTypeCheckbox.setSelection(false);
                    labelCustomValueTypeCheckbox.setEnabled(false);
                }
            }

        });

        labelCustomFieldTypeCheckbox.setSelection((labelCustomTextField
            .getText() != null)
            && (labelCustomTextField.getText().length() > 0));
        labelCustomFieldTypeCheckbox
            .setEnabled((labelCustomTextField.getText() != null)
                && (labelCustomTextField.getText().length() > 0));

        // /////////////
        labelCustomValueTypeCheckbox = new Button(composite6, SWT.CHECK
            | SWT.LEFT);
        labelCustomValueTypeCheckbox
            .addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (!labelCustomValueTypeCheckbox.getSelection())
                        labelCustomTextValue.setText("");

                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    widgetSelected(e);
                }
            });

        labelCustomTextValue = new BgcBaseText(composite6, SWT.BORDER);
        labelCustomTextValue.setLayoutData(gridData);
        labelCustomTextValue.setTextLimit(25);
        labelCustomTextValue.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if ((labelCustomTextValue.getText() != null)
                    && (labelCustomTextValue.getText().length() > 0)) {
                    labelCustomValueTypeCheckbox.setSelection(true);
                    labelCustomValueTypeCheckbox.setEnabled(true);
                } else {
                    labelCustomValueTypeCheckbox.setSelection(false);
                    labelCustomValueTypeCheckbox.setEnabled(false);
                }
            }

        });

        labelCustomValueTypeCheckbox.setSelection((labelCustomTextValue
            .getText() != null)
            && (labelCustomTextValue.getText().length() > 0));
        labelCustomValueTypeCheckbox
            .setEnabled((labelCustomTextValue.getText() != null)
                && (labelCustomTextValue.getText().length() > 0));

    }

    @SuppressWarnings("nls")
    private void createTopSection() {

        GridData gridData = new GridData(GridData.FILL, GridData.FILL, true,
            false);

        GridLayout gridLayout3 = new GridLayout();
        gridLayout3.verticalSpacing = 1;
        gridLayout3.numColumns = 1;
        gridLayout3.makeColumnsEqualWidth = true;

        Composite group3 = createSectionWithClient(
            i18n.tr("Branding"), page);
        group3.setLayoutData(gridData);
        group3.setLayout(gridLayout3);

        createTopSectionItems(group3);
    }

    @SuppressWarnings("nls")
    private void createActionButtonsGroup() {
        GridLayout gridLayout5 = new GridLayout();
        gridLayout5.numColumns = 6;
        gridLayout5.makeColumnsEqualWidth = true;

        GridData gridData = new GridData(GridData.FILL, GridData.FILL, true,
            false);

        GridData gridData7 = new GridData();
        gridData7.grabExcessHorizontalSpace = true;
        gridData7.horizontalAlignment = GridData.FILL;

        Composite group4 = createSectionWithClient(
            i18n.tr("Actions"), page);

        savePdfButton = new Button(group4, SWT.NONE);
        savePdfButton.setText(i18n.tr("Export to PDF"));
        savePdfButton.addSelectionListener(savePdfListener);
        savePdfButton.setLayoutData(gridData7);

        new Label(group4, SWT.NONE);
        new Label(group4, SWT.NONE);
        new Label(group4, SWT.NONE);
        new Label(group4, SWT.NONE);
        new Label(group4, SWT.NONE);

        group4.setLayout(gridLayout5);
        group4.setLayoutData(gridData);

    }

    /**
     * Should be called after a successful print or save.
     */
    private void updateSavePreferences() {

        perferenceStore.setValue(PreferenceConstants.LOGO_FILE_LOCATION,
            logoText.getText());
        perferenceStore.setValue(PreferenceConstants.PROJECT_TITLE,
            projectTitleText.getText());

        if (templateCombo.getSelectionIndex() >= 0)
            perferenceStore.setValue(PreferenceConstants.TEMPLATE_NAME,
                templateCombo.getItem(templateCombo.getSelectionIndex()));

        if (printerCombo.getSelectionIndex() >= 0)
            perferenceStore.setValue(PreferenceConstants.PRINTER_NAME,
                printerCombo.getItem(printerCombo.getSelectionIndex()));

        perferenceStore.setValue(PreferenceConstants.LABEL_CHECKBOX_1,
            customField1Checkbox.getSelection());
        perferenceStore.setValue(PreferenceConstants.LABEL_CHECKBOX_2,
            customField2Checkbox.getSelection());
        perferenceStore.setValue(PreferenceConstants.LABEL_CHECKBOX_3,
            customField3Checkbox.getSelection());

        perferenceStore.setValue(PreferenceConstants.LABEL_TEXT_1,
            customField1Text.getText());
        perferenceStore.setValue(PreferenceConstants.LABEL_TEXT_2,
            customField2Text.getText());
        perferenceStore.setValue(PreferenceConstants.LABEL_TEXT_3,
            customField3Text.getText());

        perferenceStore.setValue(PreferenceConstants.VALUE_CHECKBOX_1,
            customValue1Checkbox.getSelection());
        perferenceStore.setValue(PreferenceConstants.VALUE_CHECKBOX_2,
            customValue2Checkbox.getSelection());
        perferenceStore.setValue(PreferenceConstants.VALUE_CHECKBOX_3,
            customValue3Checkbox.getSelection());

        perferenceStore.setValue(PreferenceConstants.BARCODE_CHECKBOX_1,
            printBarcode1Checkbox.getSelection());
        perferenceStore.setValue(PreferenceConstants.BARCODE_CHECKBOX_2,
            printBarcode2Checkbox.getSelection());
        perferenceStore.setValue(PreferenceConstants.BARCODE_CHECKBOX_3,
            printBarcode3Checkbox.getSelection());

        perferenceStore.setValue(PreferenceConstants.SPECIMEN_TYPE_CHECKBOX,
            labelCustomFieldTypeCheckbox.getSelection());
        perferenceStore.setValue(PreferenceConstants.SPECIMEN_TYPE_TEXT,
            labelCustomTextField.getText());

    }

    public class BarcodeViewGuiData extends CBSRData {

        @SuppressWarnings("nls")
        public BarcodeViewGuiData() throws CBSRGuiVerificationException {

            projectTileStr = projectTitleText.getText();

            if ((projectTileStr == null) || (projectTileStr.length() == 0)) {
                throw new CBSRGuiVerificationException(
                    i18n.tr("Incorrect Title"),
                    i18n.tr("A valid title is required."));
            }

            ByteArrayInputStream bis = null;
            try {
                BufferedImage logoImage;

                logoImage = ImageIO.read(new File(logoText.getText()));
                ByteArrayOutputStream binaryOutputStream =
                    new ByteArrayOutputStream();
                if (logoImage != null) {
                    ImageIO.write(logoImage, "PNG", binaryOutputStream);
                    bis = new ByteArrayInputStream(
                        binaryOutputStream.toByteArray());
                } else {
                    bis = null;
                }

            } catch (IOException e) {
                bis = null;
            }
            logoStream = bis;

            fontName = perferenceStore
                .getDefaultString(PreferenceConstants.TEXT_FONT_NAME);

            if (fontName == null)
                fontName = "";

            patientNumberStr = patientNumText.getText();
            if ((patientNumberStr == null) || (patientNumberStr.length() == 0)) {
                throw new CBSRGuiVerificationException(
                    i18n.tr("Entry Error"),
                    i18n.tr("Please enter a valid patient number."));

            }
            // ------------ patient info start-----------------
            label1Str = null;
            if (customField1Checkbox.getSelection()) {
                label1Str = customField1Text.getText();

                if ((label1Str == null) || (label1Str.length() == 0)) {
                    throw new CBSRGuiVerificationException(
                        i18n.tr("Entry Error"),
                        i18n.tr("Custom field 1 contains an empty name. Please disable the name field or enter some text."));
                }

            }
            value1Str = null;
            barcode1Print = false;
            if (customValue1Checkbox.getSelection()) {
                value1Str = customValue1Text.getText();
                barcode1Print = printBarcode1Checkbox.getSelection();

                if ((value1Str == null) || (value1Str.length() == 0)) {
                    throw new CBSRGuiVerificationException(
                        i18n.tr("Entry Error"),
                        i18n.tr("Custom field 1 contains an empty value. Please disable the value field or enter some text."));
                }
            }

            label2Str = null;
            if (customField2Checkbox.getSelection()) {
                label2Str = customField2Text.getText();

                if ((label2Str == null) || (label2Str.length() == 0)) {
                    throw new CBSRGuiVerificationException(
                        i18n.tr("Entry Error"),
                        i18n.tr("Custom field 2 contains an empty name. Please disable the name field or enter some text."));
                }
            }

            value2Str = null;
            barcode2Print = false;
            if (customValue2Checkbox.getSelection()) {
                value2Str = customValue2Text.getText();
                barcode2Print = printBarcode2Checkbox.getSelection();

                if ((value2Str == null) || (value2Str.length() == 0)) {
                    throw new CBSRGuiVerificationException(
                        i18n.tr("Entry Error"),
                        i18n.tr("Custom field 2 contains an empty value. Please disable the value field or enter some text."));
                }

            }

            label3Str = null;
            if (customField3Checkbox.getSelection()) {
                label3Str = customField3Text.getText();

                if ((label3Str == null) || (label3Str.length() == 0)) {
                    throw new CBSRGuiVerificationException(
                        i18n.tr("Entry Error"),
                        i18n.tr("Custom field 3 contains an empty name. Please disable the name field or enter some text."));
                }
            }
            value3Str = null;
            barcode3Print = false;
            if (customValue3Checkbox.getSelection()) {
                value3Str = customValue3Text.getText();
                barcode3Print = printBarcode3Checkbox.getSelection();

                if ((value3Str == null) || (value3Str.length() == 0)) {
                    throw new CBSRGuiVerificationException(
                        i18n.tr("Entry Error"),
                        i18n.tr("Custom field 3 contains an empty value. Please disable the value field or enter some text."));
                }

            }
            // ------------ patient info end-----------------

            // only need if we are printing.
            if (printerCombo.getSelectionIndex() >= 0)
                printerNameStr = printerCombo.getItem(printerCombo
                    .getSelectionIndex());

            else
                printerNameStr = null;

            printBarcode2DTextBoolean = barcode2DTextCheckbox.getSelection();

            specimenTypeStr = null;
            if (labelCustomFieldTypeCheckbox.getSelection()) {
                specimenTypeStr = labelCustomTextField.getText();

                if (labelCustomValueTypeCheckbox.getSelection()) {
                    specimenTypeStr = specimenTypeStr + ": "
                        + labelCustomTextValue.getText();
                }

            }

            template = loadedTemplate;

            if (template == null) {
                throw new CBSRGuiVerificationException(
                    i18n.tr("Verification Issue"),
                    i18n.tr("Could not load template.. Selected template is null."));
            }

            if (!(template).jasperTemplateExists()) {
                throw new CBSRGuiVerificationException(
                    i18n.tr("Verification Issue"),
                    i18n.tr("Template is lacking a jasper file."));
            }
        }
    };

    private SelectionListener savePdfListener = new SelectionListener() {
        @SuppressWarnings("nls")
        @Override
        public void widgetSelected(SelectionEvent e) {
            BarcodeViewGuiData guiData = null;

            try {
                guiData = new BarcodeViewGuiData();

                // save dialog for pdf file.
                FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
                fileDialog.setFilterPath(perferenceStore.getString(
                    PreferenceConstants.PDF_DIRECTORY_PATH));
                fileDialog.setOverwrite(true);
                fileDialog.setFileName("default.pdf");
                String pdfFilePath = fileDialog.open();

                if (pdfFilePath == null)
                    return;

                List<String> specimenInventoryIds = SessionManager.getAppService().doAction(
                    new GetSourceSpecimenUniqueInventoryIdSetAction(32)).getList();

                SaveOperation saveOperation = new SaveOperation(guiData,
                    specimenInventoryIds, pdfFilePath);

                try {
                    new ProgressMonitorDialog(shell).run(true, true, saveOperation);

                } catch (InvocationTargetException e1) {
                    saveOperation.saveFailed();
                    saveOperation.setError(
                        i18n.tr("Error"),
                        i18n.tr("InvocationTargetException: ")
                            + e1.getCause().getMessage());

                } catch (InterruptedException e2) {
                    BgcPlugin.openAsyncError(
                        i18n.tr("Save error"), e2);
                }

                if (saveOperation.isSuccessful()) {
                    String parentDir = new File(pdfFilePath).getParentFile()
                        .getPath();
                    if (parentDir != null)
                        perferenceStore.setValue(
                            PreferenceConstants.PDF_DIRECTORY_PATH, parentDir);

                    updateSavePreferences();
                    clearFieldsConfirm();
                    return;
                }

                if (saveOperation.errorExists()) {
                    BgcPlugin.openAsyncError(saveOperation.getError()[0],
                        saveOperation.getError()[1]);
                }

            } catch (CBSRGuiVerificationException e1) {
                BgcPlugin.openAsyncError(e1.title, e1.messsage);
                return;
            } catch (BiobankServerException e2) {
                BgcPlugin.openAsyncError(
                    i18n.tr("Specimen ID Error"),
                    e2.getMessage());
            } catch (ApplicationException e3) {
                BgcPlugin.openAsyncError(
                    i18n.tr("Server Error"),
                    e3.getMessage());
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);

        }
    };

}
