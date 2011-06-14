package edu.ualberta.med.biobank.barcodegenerator.forms;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.PlatformUI;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.barcodegenerator.dialogs.StringInputDialog;
import edu.ualberta.med.biobank.common.wrappers.JasperTemplateWrapper;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.gui.common.BgcSessionState;
import edu.ualberta.med.biobank.gui.common.forms.BgcEntryForm;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import gov.nih.nci.system.applicationservice.ApplicationException;

/**
 * View for adding new jasper files that are mapped to a user chosen name.
 * 
 * @author Thomas Polasek 2011
 * 
 */
public class JasperConfigEntryForm extends BgcEntryForm implements
    SelectionListener {

    public static final String ID = "edu.ualberta.med.biobank.barcodegenerator.views.JasperFileEditorView";

    private Button deleteButton = null;

    private Button newButton = null;

    private Button browseButton = null;

    private BgcBaseText jasperNameText = null;

    private BgcBaseText jasperConfigText = null;

    private List list = null;

    private String loadedJasperFileXml = null;
    private String prevJasperName = null;

    private Map<String, JasperTemplateWrapper> templateMap = null;

    boolean jasperConfigDirty = false;

    private boolean loggedIn = false;

    @Override
    protected void init() throws Exception {
        setPartName("Jasper Configuration Templates");
    }

    @Override
    protected void performDoubleClick(DoubleClickEvent event) {
    }

    @Override
    protected Image getFormImage() {
        return null;
    }

    @Override
    protected void createFormContent() throws Exception {
        super.createFormContent();
        form.setText("Jasper Configuration Templates");
        form.setMessage(
            "Add Jasper configurations for different printer labels",
            IMessageProvider.NONE);
        page.setLayout(new GridLayout(1, false));

        BgcSessionState sessionSourceProvider = BgcPlugin
            .getSessionStateSourceProvider();

        loggedIn = sessionSourceProvider.getCurrentState()
            .get(BgcSessionState.SESSION_STATE_SOURCE_NAME)
            .equals(BgcSessionState.LOGGED_IN);

        createMasterDetail();

        sessionSourceProvider
            .addSourceProviderListener(new ISourceProviderListener() {
                @Override
                public void sourceChanged(int sourcePriority,
                    String sourceName, Object sourceValue) {
                    if (sourceValue != null) {
                        loggedIn = sourceValue
                            .equals(BgcSessionState.LOGGED_IN);
                        updateForm();
                    }
                }

                @Override
                public void sourceChanged(int sourcePriority,
                    @SuppressWarnings("rawtypes") Map sourceValuesByName) {
                }
            });

        updateForm();
    }

    @Override
    public void setFocus() {
    }

    private void createMasterDetail() {
        page.setLayout(new GridLayout(2, false));
        page.setLayoutData(new GridData(GridData.FILL, GridData.FILL | SWT.TOP,
            true, true));

        // master section
        Composite masterComp = toolkit.createComposite(page);
        masterComp.setLayout(new GridLayout(1, false));
        masterComp.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
            true, true));

        Composite client = createSectionWithClient("Jasper Configurations",
            masterComp);
        client.setLayout(new GridLayout());

        list = new List(client, SWT.BORDER | SWT.V_SCROLL);
        list.addSelectionListener(listListener);
        GridData gd = new GridData();
        gd.grabExcessVerticalSpace = true;
        gd.verticalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = GridData.FILL;
        gd.heightHint = 200;
        gd.widthHint = 125;
        list.setLayoutData(gd);

        Composite buttonComp = toolkit.createComposite(client);
        buttonComp.setLayout(new GridLayout(2, true));
        buttonComp.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
            true, true));

        newButton = toolkit.createButton(buttonComp, "New", SWT.NONE);
        newButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
            true, true));
        newButton.addSelectionListener(this);

        deleteButton = toolkit.createButton(buttonComp, "Delete", SWT.NONE);
        deleteButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
            true, true));
        deleteButton.addSelectionListener(this);

        createConfigDetailsSection();
    }

    private void createConfigDetailsSection() {
        Composite detailsComp = toolkit.createComposite(page);
        detailsComp.setLayout(new GridLayout(1, false));
        detailsComp.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
            true, true));

        Composite client = createSectionWithClient(
            "Jasper Configuration Details", detailsComp);
        client.setLayout(new GridLayout(3, false));
        client.setLayoutData(new GridData(GridData.FILL, GridData.FILL
            | SWT.TOP, true, true));

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;

        jasperNameText = (BgcBaseText) createLabelledWidget(client,
            BgcBaseText.class, SWT.NONE, "Configuration Name");
        jasperNameText.setLayoutData(gd);
        jasperNameText.setEditable(false);

        jasperConfigText = (BgcBaseText) createLabelledWidget(client,
            BgcBaseText.class, SWT.NONE, "Jasper File");
        jasperConfigText.setEditable(false);
        jasperConfigText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        browseButton = toolkit.createButton(client, "Browse...", SWT.PUSH);
        browseButton.addSelectionListener(this);
    }

    private void setEnable(boolean enable) {
        deleteButton.setEnabled(enable);
        browseButton.setEnabled(enable);
        newButton.setEnabled(enable);
    }

    private void updateForm() {
        if (templateMap == null) {
            templateMap = new HashMap<String, JasperTemplateWrapper>();
        }

        try {

            if (loggedIn) {
                setEnable(true);

                list.setEnabled(true);

                for (JasperTemplateWrapper t : JasperTemplateWrapper
                    .getAllTemplates(SessionManager.getAppService())) {
                    String name = t.getName();
                    templateMap.put(name, t);
                    list.add(name);
                }
                list.redraw();
            } else {

                setEnable(false);

                templateMap.clear();

                list.removeAll();
                list.setEnabled(false);
                list.redraw();
            }
        } catch (ApplicationException e) {
            BgcPlugin.openAsyncError("Database Error",
                "Error while updating form", e);
        }
    }

    /**
     * Sets the name of the selected jasper template list to prevJasperName.
     * 
     */
    private SelectionListener listListener = new SelectionListener() {
        @Override
        public void widgetSelected(SelectionEvent e) {
            try {
                String[] selectedItems = list.getSelection();
                if (selectedItems.length == 1) {
                    confirm();

                    if (selectedItems[0] != null) {
                        JasperTemplateWrapper selectedTemplate = (templateMap
                            .get(selectedItems[0]));
                        jasperNameText.setText(selectedTemplate.getName());
                        if (selectedTemplate.getXml() == null
                            || selectedTemplate.getXml().isEmpty()) {
                            jasperConfigText
                                .setText("Please select a Jasper file");
                        } else
                            jasperConfigText.setText("Jasper file loaded");

                        prevJasperName = selectedItems[0];
                        jasperConfigDirty = false;

                    } else {
                        jasperNameText.setText("Please select a template");
                        jasperConfigText.setText("");
                        prevJasperName = null;
                        jasperConfigDirty = false;
                    }

                } else {
                    BgcPlugin.openAsyncError("Selection Listener Error",
                        "invalid selected items length: "
                            + selectedItems.length);
                }
            } catch (Exception e1) {
                BgcPlugin.openAsyncError("Jasper Template Save Error",
                    "could not save template to database", e1);
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);

        }
    };

    public void confirm() {
        try {
            if (prevJasperName != null) {
                if (jasperConfigDirty) {
                    if (BgcPlugin
                        .openConfirm("Jasper Configuration Editor Saving",
                            "Jasper Configuration has been modified, do you want to save your changes?")) {

                        JasperTemplateWrapper selectedTemplate = (templateMap
                            .get(prevJasperName));

                        if (loadedJasperFileXml != null) {
                            selectedTemplate.setXml(loadedJasperFileXml);
                            loadedJasperFileXml = null;
                        }

                        if (selectedTemplate.getXml() != null
                            && !selectedTemplate.getXml().isEmpty()) {
                            selectedTemplate.persist();
                            jasperConfigText.setText("Jasper file loaded");

                        } else {
                            jasperConfigText
                                .setText("Please select a Jasper file");
                            throw new Exception("Jasper file was not selected");
                        }
                    }
                    jasperConfigDirty = false;
                }
            }
        } catch (Exception e1) {
            BgcPlugin.openAsyncError("Template Save Error",
                "Could not save the template to the database", e1);
        }
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (e.getSource() == newButton) {
            StringInputDialog dialog = new StringInputDialog(
                "New Jasper Configuration Name",
                "What is the name of this new Jasper Configuration?", "Name",
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
            if (dialog.open() == Dialog.OK) {
                String jasperConfigName = dialog.getValue();

                if (!templateMap.containsKey(jasperConfigName)) {
                    JasperTemplateWrapper newTemplate = new JasperTemplateWrapper(
                        SessionManager.getAppService());

                    try {
                        newTemplate.setName(jasperConfigName);
                        templateMap.put(jasperConfigName, newTemplate);
                        list.add(jasperConfigName);

                        list.redraw();
                    } catch (Exception e1) {
                        BgcPlugin.openAsyncError(
                            "Failed to Save",
                            "Faile to save newly created template: "
                                + e1.getMessage());
                    }

                } else {
                    BgcPlugin
                        .openAsyncError("Jasper Configuration Exists",
                            "Your new Jasper Configuration must have a unique name.");
                }
            }
        } else if (e.getSource() == deleteButton) {
            try {

                if (list.getSelectionCount() == 1 && prevJasperName != null) {
                    JasperTemplateWrapper selected = templateMap
                        .get(prevJasperName);

                    if (selected != null) {
                        MessageBox messageBox = new MessageBox(PlatformUI
                            .getWorkbench().getActiveWorkbenchWindow()
                            .getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                        messageBox
                            .setMessage("Are you sure you want to delete "
                                + selected.getName() + "?");
                        messageBox.setText("Deleting Jasper Configuration");

                        int response = messageBox.open();
                        if (response == SWT.YES) {

                            if (!selected.isNew())
                                selected.delete();
                            templateMap.remove(prevJasperName);
                            list.remove(prevJasperName);

                            jasperNameText.setText("Please select a template");
                            jasperConfigText.setText("");
                            prevJasperName = null;
                            jasperConfigDirty = false;

                            list.deselectAll();
                            list.redraw();
                        }
                    }
                }
            } catch (Exception e1) {
                BgcPlugin
                    .openAsyncError(
                        "Template Delete Error",
                        "Could not delete template. A printer template is using this jasper configuration.",
                        e1);
            }
        } else if (e.getSource() == browseButton) {
            FileDialog fd = new FileDialog(PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getShell(), SWT.OPEN);
            fd.setText("Select Jasper File");
            String[] filterExt = { "*.jrxml" };
            fd.setFilterExtensions(filterExt);
            String path = fd.open();
            if (path != null) {
                try {
                    File f = new File(path);
                    if (f.exists()) {
                        loadedJasperFileXml = FileUtils.readFileToString(f);
                        jasperConfigText.setText(path);
                        jasperConfigDirty = true;
                    } else {
                        BgcPlugin.openAsyncError("File Not Found",
                            "File selected does not exist.");
                    }

                } catch (IOException e1) {
                    BgcPlugin.openAsyncError("Template Read Error",
                        "Could not open the template file for reading", e1);
                }
            }
        } else {
            BgcPlugin.openAsyncError("Invalid selection event",
                "invalid selection source");
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
    }

}
