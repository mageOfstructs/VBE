package vbe.usersystem;

import newworld.fs.IDirForCommercialUse;
import vbe.exceptions.fs.FsElementNotFoundException;
import vbe.exceptions.fs.WrongElementType;
import vbe.exceptions.usersystem.AccessDeniedException;

public interface IUser {

	String getName();

	void setPassword(String passwd);

	int getId();

	String getPassword();

	String getHomePath();

	IDirForCommercialUse getHome();

	String[] getGroupNames();

	void setUserHome(IDirForCommercialUse rootHome) throws FsElementNotFoundException, WrongElementType;

	IGroup getPrimaryGroup();

	void addGroup(IGroup newGroup) throws FsElementNotFoundException, WrongElementType;

	IGroup[] getGroups();

	void setPrimGroup(IGroup group) throws FsElementNotFoundException, WrongElementType;

	void setName(String string);

	void setLocked(boolean b);

	void remGroup(IGroup iGroup) throws FsElementNotFoundException, WrongElementType;

	boolean isLocked();

	boolean performPasswdCheck() throws AccessDeniedException;

	void updateConfigFile() throws FsElementNotFoundException, WrongElementType;

	void setComment(String comment) throws FsElementNotFoundException, WrongElementType;

	void setUserHome(IDirForCommercialUse rootHome, boolean b) throws FsElementNotFoundException, WrongElementType;

	String getComment();

	void setLineIndex(int i);

	int getDaysSinceLastPasChange();

	int getMinPasAge();

	int getMaxPasAge();

	int getWarnPeriod();

	int getInactivePeriod();

	int getExpirationDate();

	void clearGroups() throws FsElementNotFoundException, WrongElementType;
}
