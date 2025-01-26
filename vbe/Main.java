package vbe;

import java.io.IOException;

import newworld.fs.FileSystem;
import newworld.fs.IDirForCommercialUse;
import vbe.commands.CommandHandler;
import vbe.commands.Commands;
import vbe.usersystem.UserSystem;

public class Main {
	
	public static void main(String[] args) {
		FileSystem.init(args);
		
		System.out.println("Welcome to the Virtual BASH Environment! Enter 'help' to view the available commands.");
		String command = "";
		while (!Commands.canExit()) {
			IDirForCommercialUse curDir = FileSystem.getOnlyInstance().getCurDir();
			System.out.print("[" + UserSystem.curUser.getName() + "@" + FileSystem.getOnlyInstance().getHostname() + " " + (curDir.getPath().equals(UserSystem.curUser.getHomePath()) ? "~" : curDir.getName().isEmpty() ? "/" : curDir.getName()) + "]" + (UserSystem.isRoot() ? "#" : "$") + " ");
			try {
				command = CommandHandler.handleInput();
				CommandHandler.handleCommand(command);
			} catch (IOException e) {
				System.out.println(GlobalVars.ERR_PREFIX + "Error while reading user input stream: " + e.getMessage());
			}
		}
	}
}