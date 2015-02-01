package sk.lkce.techforms;

public class FirmyRecord {

	private String shortName;
	private String fullName;
	private String technicianName;
	private String technicianPhone;
	private String technicianEmail;

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getTechnicianName() {
		return technicianName;
	}

	public void setTechnicianName(String technicianName) {
		this.technicianName = technicianName;
	}

	public String getTechnicianPhone() {
		return technicianPhone;
	}

	public void setTechnicianPhone(String technicianPhone) {
		this.technicianPhone = technicianPhone;
	}

	public String getTechnicianEmail() {
		return technicianEmail;
	}

	public void setTechnicianEmail(String technicianEmail) {
		this.technicianEmail = technicianEmail;
	}

	@Override
	public String toString() {
		return FirmyRecord.class.getSimpleName() + " [shortName=" + shortName + ", fullName=" + fullName
				+ ", technicianName=" + technicianName + ", technicianPhone="
				+ technicianPhone + ", technicianEmail=" + technicianEmail
				+ "]";
	}

	
}
