package vulc.luag.game;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import vulc.luag.Console;
import vulc.luag.sfx.Sound;

public class GameSounds {

	private final HashMap<String, Sound> list = new HashMap<String, Sound>();

	public Sound get(String name) {
		return list.get(name);
	}

	public void remove() {
		Set<String> keys = list.keySet();
		for(String key : keys) {
			list.get(key).stop();
		}
	}

	public boolean init(Console console) {
		File sfxDir = new File(Game.SFX_DIR);
		if(!sfxDir.isDirectory()) {
			console.die("Error:\n"
			            + "'" + Game.SFX_NAME + "'\n"
			            + "folder not found");
			return false;
		}

		readSoundsInFolder(sfxDir, "");

		return true;
	}

	private void readSoundsInFolder(File folder, String relativeToSfxRoot) {
		File[] files = folder.listFiles();
		for(File file : files) {
			String fileName = file.getName();

			if(file.isDirectory()) {
				readSoundsInFolder(file, relativeToSfxRoot + fileName + "/");
			} else {
				if(fileName.endsWith(".wav")) {
					String name = relativeToSfxRoot + fileName;
					name = name.substring(0, name.lastIndexOf('.'));
					try {
						list.put(name, new Sound(file.toURI().toURL()));
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public boolean initAsCartridge(ZipFile zipFile, List<ZipEntry> entries) {
		return true;
		// TODO sound from cartridges
//		for(ZipEntry entry : entries) {
//			String fileName = entry.getName();
//
//			if(!fileName.startsWith(Game.SFX_NAME)
//			   || !fileName.endsWith(".wav")) continue;
//
//			String sfxName = fileName.substring(fileName.indexOf("/") + 1);
//			try {
//				list.put(sfxName, new Sound(zipFile.getInputStream(entry)));
//			} catch(IOException e) {
//				e.printStackTrace();
//			}
//		}
	}

}
