package edu.ualberta.med.biobank.barcodegenerator.template.presets;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class CBSRData {
	public String projectTileStr;
	public ByteArrayInputStream logoStream;

	public String patientIdStr;

	public String label1Str;
	public String value1Str;
	public boolean barcode1Print;

	public String label2Str;
	public String value2Str;
	public boolean barcode2Print;

	public String label3Str;
	public String value3Str;
	public boolean barcode3Print;

	public String sampleTypeStr;
}