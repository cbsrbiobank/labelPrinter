package edu.ualberta.med.biobank.barcodegenerator.template.presets.cbsr.exceptions;

public class CBSRGuiVerificationException extends Exception {

    private static final long serialVersionUID = -591112207091663347L;

    public String title, messsage;

    public CBSRGuiVerificationException(String title, String message) {
        this(title + " : " + message);
        this.title = title;
        this.messsage = message;
    }

    public CBSRGuiVerificationException(String message) {
        super(message);
    }
};