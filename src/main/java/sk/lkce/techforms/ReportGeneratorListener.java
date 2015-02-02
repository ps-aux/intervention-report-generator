package sk.lkce.techforms;

public interface ReportGeneratorListener {

	void warningIssued(String msg);
	
	void messageIssued(String msg);
	
	void statusMsgChanged(String msg);
	
	void generationFinished(String msg);
	
}
