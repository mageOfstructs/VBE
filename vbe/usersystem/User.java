package vbe.usersystem;

import java.io.Serializable;
import java.util.ArrayList;

import newworld.fs.FileSystem;
import newworld.fs.FileSystem.FS_CONFIGS;
import newworld.fs.IDirForCommercialUse;
import vbe.GlobalVars;
import vbe.SystemElement;
import vbe.commands.CommandHandler;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.fs.WrongElementType;
import vbe.exceptions.usersystem.AccessDeniedException;

public class User extends SystemElement implements IUser, Serializable {
	private static final long serialVersionUID = 9105748222052470519L;
	private String password;
	private IDirForCommercialUse userHome;
	private ArrayList<IGroup> groups = new ArrayList<>();
	private int id;
	private String comment;
	private int lineIndex = -1;
	
	/* shadow specific */
	private int daysSinceLastPasChange = -1, minPasAge = -1, maxPasAge = -1, warnPeriod = -1, inactivePeriod = -1, expirationDate = -1, shadowLineIndex = -1;
	
	public User(String name, String password, IDirForCommercialUse userHome, String comment, IGroup... groups) {
		super(name);
		this.password = password;
		this.userHome = userHome;
		this.comment = comment;
		id = UserSystem.getUserId();
		for (int i = 0; i < groups.length; i++)
			this.groups.add(groups[i]);
		daysSinceLastPasChange = FileSystem.getDateInDays();
	}

	public User(String name, String password, int uid, String comment, IDirForCommercialUse userHome, int passwdLineIndex, int shadowLineIndex, IGroup...groups) {
		super(name);
		this.password = password;
		this.userHome = userHome;
		this.id = uid;
		this.comment = comment;
		this.lineIndex = passwdLineIndex;
		this.shadowLineIndex = shadowLineIndex;
		for (IGroup g : groups)
			this.groups.add(g);
	}

	public String[] getGroupNames() {
		String[] ret = new String[groups.size()];
		for (int i = 0; i < groups.size(); i++)
			ret[i] = groups.get(i).getName();
		return ret;
	}
	
	public void addGroup(IGroup group) throws FsElementNotFoundException, WrongElementType {
		groups.add(group);
		group.addUser(this);
	}
	
	public String getHomePath() { if (userHome != null) return userHome.getPath(); else return ""; }
	public IDirForCommercialUse getHome() { return userHome; }
	
	public String getPassword() { return password; }
	public void setPassword(String newPasswd) { 
		password = newPasswd;
		daysSinceLastPasChange = FileSystem.getDateInDays();
	}
	
	public IGroup getPrimaryGroup() { return groups.get(0); }
	
	@Override
	public void setPrimGroup(IGroup group) throws FsElementNotFoundException, WrongElementType { groups.set(0, group); updateConfigFile(); }
	
	public String toString() { return "[User] " + getName(); }

	public void setUserHome(IDirForCommercialUse newHome) throws FsElementNotFoundException, WrongElementType { setUserHome(newHome, true); }
	public void setUserHome(IDirForCommercialUse newHome, boolean update) throws FsElementNotFoundException, WrongElementType { 
		userHome = newHome;
		if (update) updateConfigFile();
	}
	
	public int getId() { return id; }

	@Override
	public IGroup[] getGroups() {
		IGroup[] ret = new IGroup[groups.size()];
		for (int i = 0; i < ret.length; i++)
			ret[i] = groups.get(i);
		return ret;
	}

	@Override
	public void setLocked(boolean b) { 
		if (b) expirationDate = FileSystem.getDateInDays();
		else expirationDate = -1;
	}

	public boolean isLocked() { return expirationDate != -1; }
	
	@Override
	public void remGroup(IGroup group) throws FsElementNotFoundException, WrongElementType {
		groups.remove(group);
		group.remUser(this);
	}
	
	public boolean performPasswdCheck() throws AccessDeniedException {
		int i = 0;
		System.out.print("[" + CommandHandler.getComName() + "] password for " + getName() + ": ");
		String passwd = GlobalVars.getIn().next();
		while (i < 2 && !passwd.equals(getPassword())) {
			System.out.println("Sorry, try again");
			System.out.print("[" + CommandHandler.getComName() + "] password for " + getName() + ": ");
			passwd = GlobalVars.getIn().next();
			i++;
		}
		if (!passwd.equals(getPassword())) throw new AccessDeniedException("3 incorrect password attempts");
		return true;
	}
	
	public void updateConfigFile() throws FsElementNotFoundException, WrongElementType {
		String linePasswd = name + ":x:" + id + ":" + groups.get(0).getId() + ":" + comment + ":" + (getHomePath().isEmpty() ? "/" : getHomePath()) + ":/bin/bash";
		StringBuilder lineShadow = new StringBuilder(name); lineShadow.append(":");
		if (password != null && !password.isEmpty()) lineShadow.append(password);
		else lineShadow.append("!*");
		lineShadow.append(":");
		if (daysSinceLastPasChange != -1) lineShadow.append(daysSinceLastPasChange);
		lineShadow.append(":");
		if (minPasAge != -1) lineShadow.append(minPasAge);
		lineShadow.append(":");
		if (maxPasAge != -1) lineShadow.append(maxPasAge);
		lineShadow.append(":");
		if (warnPeriod != -1) lineShadow.append(warnPeriod);
		lineShadow.append(":");
		if (inactivePeriod != -1) lineShadow.append(inactivePeriod);
		lineShadow.append(":");
		if (expirationDate != -1) lineShadow.append(expirationDate);
		
		if (lineIndex == -1) { 
			FileSystem.getOnlyInstance().addLine(FS_CONFIGS.FS_PASSWD, false, linePasswd);
			lineIndex = FileSystem.getOnlyInstance().length(FS_CONFIGS.FS_PASSWD)-1;
		} else FileSystem.getOnlyInstance().setLine(FS_CONFIGS.FS_PASSWD, lineIndex, linePasswd);
		
		if (shadowLineIndex == -1) {
			FileSystem.getOnlyInstance().addLine(FS_CONFIGS.FS_SHADOW, false, lineShadow.toString());
			shadowLineIndex = FileSystem.getOnlyInstance().length(FS_CONFIGS.FS_SHADOW)-1;
		} else FileSystem.getOnlyInstance().setLine(FS_CONFIGS.FS_SHADOW, shadowLineIndex, lineShadow.toString());
	}

	@Override
	public void setComment(String comment) throws FsElementNotFoundException, WrongElementType {
		this.comment = comment;
		updateConfigFile();
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public void setLineIndex(int i) {
		lineIndex = i;
	}

	public int getDaysSinceLastPasChange() {
		return daysSinceLastPasChange;
	}

	public void setDaysSinceLastPasChange(int daysSinceLastPasChange) {
		this.daysSinceLastPasChange = daysSinceLastPasChange;
	}

	public int getMinPasAge() {
		return minPasAge;
	}

	public void setMinPasAge(int minPasAge) {
		this.minPasAge = minPasAge;
	}

	public int getMaxPasAge() {
		return maxPasAge;
	}

	public void setMaxPasAge(int maxPasAge) {
		this.maxPasAge = maxPasAge;
	}

	public int getWarnPeriod() {
		return warnPeriod;
	}

	public void setWarnPeriod(int warnPeriod) {
		this.warnPeriod = warnPeriod;
	}

	public int getInactivePeriod() {
		return inactivePeriod;
	}

	public void setInactivePeriod(int inactivePeriod) {
		this.inactivePeriod = inactivePeriod;
	}

	public int getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(int expirationDate) {
		this.expirationDate = expirationDate;
	}

	@Override
	public void clearGroups() throws FsElementNotFoundException, WrongElementType {
		for (IGroup group : groups) group.remUser(this);
		groups.clear();
	}
}
