package vulc.luag.shell;

import java.util.ArrayList;
import java.util.List;

import vulc.luag.Console;
import vulc.luag.gfx.Screen;
import vulc.luag.gfx.panel.ShellPanel;
import vulc.luag.shell.command.ShellCommand;

public abstract class Shell {

	private static final int BACKGROUND = 0x000000;
	private static final int FOREGROUND = 0xffffff;

	public static final int HORIZONTAL_CHARS = Console.WIDTH / (Screen.FONT.widthOf(' ') + 1);
	private static final int VERTICAL_LINES = Console.HEIGHT / (Screen.FONT.getHeight() + 1);

	public static final List<ShellChar> CHAR_BUFFER = new ArrayList<ShellChar>();
	public static int scrollBuffer = 0;
	public static boolean pressedUP = false, pressedLEFT = false, pressedDOWN = false, pressedRIGHT = false;

	private static final List<String> CLOSED_TEXT = new ArrayList<String>();
	private static String currentLine = "";
	private static int currentChar = 0;

	private static final List<String> COMMAND_HISTORY = new ArrayList<String>();
	private static int historyPoint = 0;

	private static int renderOffset = 0;
	private static int animationTicks = 0; // cursor animation

	public static ShellPanel panel;

	private Shell() {
	}

	public static void init() {
		write(Console.NAME + "\n");
		write(Console.COPYRIGHT + "\n");
		write("Version: " + Console.VERSION + "\n");
		write("\n");
	}

	public static void tick() {
		if(CHAR_BUFFER.size() != 0) {
			ShellChar character = CHAR_BUFFER.remove(0);
			if(receiveInput(character.val, character.writtenByUser)) {
				animationTicks = 0;
			}
		} else {
			animationTicks++;
		}

		// history
		byte historyShift = 0;
		if(pressedUP) {
			pressedUP = false;
			historyShift++;
		}
		if(pressedDOWN) {
			pressedDOWN = false;
			historyShift--;
		}

		if(historyShift != 0) {
			int newHistoryPoint = historyPoint + historyShift;

			if(newHistoryPoint >= 0 && newHistoryPoint < COMMAND_HISTORY.size()) {
				historyPoint = newHistoryPoint;

				// most recent command is 0
				currentLine = COMMAND_HISTORY.get(historyPoint);
				currentChar = currentLine.length();
			}
		}

		if(scrollBuffer != 0) {
			int newOffset = renderOffset + scrollBuffer;

			// if is under the bottom then move back to bottom
			int lowestOffset = lowestOffset();
			if(newOffset > lowestOffset) {
				newOffset = lowestOffset;
			}

			// this may be caused by the previous if block
			if(newOffset < 0) newOffset = 0;

			renderOffset = newOffset;
			scrollBuffer = 0;
		}

		if(pressedLEFT) {
			pressedLEFT = false;
			if(currentChar > 0) currentChar--;
		}
		if(pressedRIGHT) {
			pressedRIGHT = false;
			if(currentChar < currentLine.length()) currentChar++;
		}

		render();
	}

	private static void render() {
		Console.SCREEN.clear(BACKGROUND);

		String textToRender = "";
		for(int i = 0; i < VERTICAL_LINES; i++) {
			int line = i + renderOffset;

			if(line < CLOSED_TEXT.size()) {
				// closed lines always end with '\n'
				textToRender += CLOSED_TEXT.get(line);
			} else {
				String text = currentLine;

				String[] splitCurrent = splitCurrentLine(text);
				for(int a = 0; a < splitCurrent.length; a++) {
					textToRender += splitCurrent[a];

					if(a != splitCurrent.length - 1) {
						textToRender += "\n";
					}
				}
				break;
			}
		}
		Console.SCREEN.write(textToRender, FOREGROUND, 1, 1);

		// draw the _ cursor
		if(animationTicks / 25 % 2 == 1) {
			Console.SCREEN.write("_", FOREGROUND,
			                     1 + (Screen.FONT.widthOf(' ') + Screen.FONT.getLetterSpacing())
			                         * ((currentChar) % HORIZONTAL_CHARS),
			                     1 + (Screen.FONT.getHeight() + Screen.FONT.getLineSpacing())
			                         * (CLOSED_TEXT.size() - renderOffset + (currentChar) / HORIZONTAL_CHARS));
		}
	}

	// returns true if 'isWriting' should be set to true
	public static boolean receiveInput(char character, boolean shouldExecute) {
		int lowestOffset;
		switch(character) {
			case '\n':
				if(shouldExecute) execute(currentLine);

				String[] splitCurrent = splitCurrentLine(currentLine);
				for(int i = 0; i < splitCurrent.length; i++) {
					// here the text always ends with '\n'
					CLOSED_TEXT.add(splitCurrent[i] + "\n");
				}
				currentLine = "";
				currentChar = 0;

				// if is not at bottom then move to bottom
				lowestOffset = lowestOffset();
				if(renderOffset < lowestOffset) {
					renderOffset = lowestOffset;
				}
				return true;

			case '\b':
				if(currentLine.length() > 0 && currentChar > 0) {
					if(panel.ctrl.isKeyDown()) {
						int lastSpace = -1;
						boolean foundNonSpace = false;
						for(int i = currentChar - 1; i >= 0; i--) {
							if(currentLine.charAt(i) == ' ') {
								if(foundNonSpace) {
									lastSpace = i;
									break;
								}
							} else {
								foundNonSpace = true;
							}
						}

						int oldLength = currentLine.length();
						currentLine = currentLine.substring(0, lastSpace + 1)
						              + currentLine.substring(currentChar, currentLine.length());

						currentChar -= oldLength - currentLine.length();
					} else {
						currentLine = currentLine.substring(0, currentChar - 1)
						              + currentLine.substring(currentChar, currentLine.length());
						currentChar--;
					}
				}
				return true;

			case 127:
				if(currentLine.length() > 0 && currentChar != currentLine.length()) {
					if(panel.ctrl.isKeyDown()) {
						// TODO ctrl+del
					} else {
						currentLine = currentLine.substring(0, currentChar)
						              + currentLine.substring(currentChar + 1, currentLine.length());
					}
				}
				return true;

			default:
				if(character >= 32 && character < 127) {
					currentLine = currentLine.substring(0, currentChar)
					              + character
					              + currentLine.substring(currentChar, currentLine.length());
					currentChar++;

					// if is not at bottom then move to bottom
					lowestOffset = lowestOffset();
					if(renderOffset < lowestOffset) {
						renderOffset = lowestOffset;
					}
					return true;
				}
		}
		return false;
	}

	public static void write(String text) {
		for(int i = 0; i < text.length(); i++) {
			CHAR_BUFFER.add(new ShellChar(text.charAt(i), false));
		}
	}

	public static void clear() {
		CLOSED_TEXT.clear();
		currentLine = "";
		renderOffset = 0;
	}

	public static void execute(String line) {
		COMMAND_HISTORY.add(0, line);
		historyPoint = -1;

		line = line.trim();
		if(!ShellCommand.execute(line)) {
			write("unknown command\n\n");
		}
	}

	private static String[] splitCurrentLine(String line) {
		int height = (line.length() - 1) / (Shell.HORIZONTAL_CHARS) + 1;

		String[] splitLine = new String[height];
		for(int i = 0; i < height; i++) {
			int start = i * Shell.HORIZONTAL_CHARS;

			int end = start;
			if(i != height - 1) {
				end += Shell.HORIZONTAL_CHARS;
			} else {
				// if length == HORIZONTAL_CHARS the % will be 0
				if(line.length() != 0
				   && line.length() % Shell.HORIZONTAL_CHARS == 0) {
					end += Shell.HORIZONTAL_CHARS;
				} else {
					end += line.length() % (Shell.HORIZONTAL_CHARS);
				}
			}
			splitLine[i] = line.substring(start, end);
		}
		return splitLine;
	}

	private static int lowestOffset() {
		int currentLineHeight = currentLine.length() / HORIZONTAL_CHARS + 1;
		return CLOSED_TEXT.size() - VERTICAL_LINES + currentLineHeight;
	}

}
