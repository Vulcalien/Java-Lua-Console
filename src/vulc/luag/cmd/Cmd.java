package vulc.luag.cmd;

import java.util.ArrayList;
import java.util.List;

import vulc.luag.Console;
import vulc.luag.gfx.Screen;
import vulc.luag.gfx.panel.CmdPanel;
import vulc.luag.gfx.panel.EditorPanel;
import vulc.luag.gfx.panel.GamePanel;

public class Cmd {

	private final int background = 0x000000;
	private final int foreground = 0xffffff;

	private final List<String> closedLines = new ArrayList<String>();
	public final List<CmdChar> charBuffer = new ArrayList<CmdChar>();
	private String currentLine = "";

	private int animationTicks = 0;

	private Console console;
	public CmdPanel cmdPanel;

	public Cmd(Console console) {
		this.console = console;
	}

	public void tick() {
		boolean writing = false;
		if(charBuffer.size() != 0) {
			CmdChar character = charBuffer.remove(0);
			receiveInput(character.val, character.writtenByUser);
			writing = true;
		} else {
			animationTicks++;
		}

		console.screen.clear(background);
		for(int i = 0; i <= closedLines.size(); i++) {
			String text;
			if(i != closedLines.size()) {
				text = closedLines.get(i);
			} else {
				text = currentLine;
				if(!writing && animationTicks / 25 % 2 == 1) {
					text += "_";
				}
			}
			console.screen.write(text, foreground, 1, 1 + i * (Screen.FONT.getHeight() + 1));
		}
	}

	public void init() {
		write(Console.NAME + "\n");
		write(Console.COPYRIGHT + "\n");
		write("Version: " + Console.VERSION + "\n");
		write("\n");
	}

	public void receiveInput(char character, boolean shouldExecute) {
		switch(character) {
			case '\n':
				if(shouldExecute) execute(currentLine);
				closedLines.add(currentLine);
				currentLine = "";
				break;

			case '\b':
				if(currentLine.length() > 0) {
					currentLine = currentLine.substring(0, currentLine.length() - 1);
				}
				break;

			default:
				if(character >= 32 && character < 127) {
					currentLine += character;
				}
				break;
		}
	}

	public void write(String text) {
		for(int i = 0; i < text.length(); i++) {
			charBuffer.add(new CmdChar(text.charAt(i), false));
		}
	}

	public void execute(String command) {
		command = command.trim();
		switch(command) {
			case "run":
				console.switchToPanel(new GamePanel(console));
				break;

			case "edit":
			case "editor":
				console.switchToPanel(new EditorPanel(console));
				break;

			case "cls":
				closedLines.clear();
				currentLine = "";
				break;

			case "ver":
			case "version":
				write(Console.VERSION + " - By Vulcalien\n\n");
				break;

			default:
				write("unknown command\n\n");
				break;
		}
	}

}
