package vulc.luag.game.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipFile;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import vulc.luag.Console;
import vulc.luag.game.Game;
import vulc.luag.gfx.Screen;
import vulc.luag.sfx.Sound;

public class LuaInterface {

	private Console console;
	private Game game;
	private Globals env;

	public void init(Console console, Game game, Globals env) {
		this.console = console;
		this.game = game;
		this.env = env;

		// SCREEN
		env.set("scr_w", console.screen.width);
		env.set("scr_h", console.screen.height);

		// FONT
		env.set("font_w", Screen.FONT.widthOf(' '));
		env.set("font_h", Screen.FONT.getHeight());

		// MAP
		env.set("map_w", game.map.width);
		env.set("map_h", game.map.height);

		// FUNCTIONS
		// general
		env.set("loadscript", new loadscript());

		// keys
		env.set("key", new key());
		env.set("key_down", new key());
		env.set("key_pressed", new key_pressed());
		env.set("key_released", new key_released());

		// sound
		env.set("sfx", new sfx());
		env.set("sfx_play", new sfx());
		env.set("sfx_loop", new sfx_loop());
		env.set("sfx_stop", new sfx_stop());

		// screen
		env.set("settransparent", new settransparent());
		env.set("clear", new clear());
		env.set("pix", new pix());
		env.set("write", new write());
		env.set("spr", new spr());

		// map
		env.set("get_tile", new get_tile());
		env.set("set_tile", new set_tile());
		env.set("maprender", new maprender());
	}

	//---GENERAL---\\

	private class loadscript extends OneArgFunction {
		public LuaValue call(LuaValue script) {
			if(console.cartridge == null) {
				env.loadfile(Game.SCRIPT_DIR + "/" + script).call();
			} else {
				String buffer = "";
				try {
					ZipFile cartridgeFile = game.cartridgeFile;
					InputStream in = cartridgeFile.getInputStream(cartridgeFile.getEntry(Game.SCRIPT_NAME
					                                                                     + "/" + script));
					BufferedReader reader = new BufferedReader(new InputStreamReader(in));

					String line = "";
					while((line = reader.readLine()) != null) {
						buffer += line + '\n';
					}
				} catch(IOException e) {
					e.printStackTrace();
				}

				env.load(buffer).call();
			}
			return NIL;
		}
	}

	//---KEYS---\\

	private class key extends OneArgFunction {
		public LuaValue call(LuaValue id) {
			return valueOf(game.keys.get(id.checkint()).isKeyDown());
		}
	}

	private class key_pressed extends OneArgFunction {
		public LuaValue call(LuaValue id) {
			return valueOf(game.keys.get(id.checkint()).isPressed());
		}
	}

	private class key_released extends OneArgFunction {
		public LuaValue call(LuaValue id) {
			return valueOf(game.keys.get(id.checkint()).isReleased());
		}
	}

	//---SOUND---\\

	private class sfx extends OneArgFunction {
		public LuaValue call(LuaValue name) {
			Sound sound = game.sounds.get(name.checkjstring());
			if(sound == null) {
				System.err.println("Error: sound '" + name + "' does not exist");
			} else {
				sound.play();
			}
			return NIL;
		}
	}

	private class sfx_loop extends OneArgFunction {
		public LuaValue call(LuaValue name) {
			Sound sound = game.sounds.get(name.checkjstring());
			if(sound == null) {
				System.err.println("Error: sound '" + name + "' does not exist");
			} else {
				sound.loop();
			}
			return NIL;
		}
	}

	private class sfx_stop extends OneArgFunction {
		public LuaValue call(LuaValue name) {
			Sound sound = game.sounds.get(name.checkjstring());
			if(sound == null) {
				System.err.println("Error: sound '" + name + "' does not exist");
			} else {
				sound.stop();
			}
			return NIL;
		}
	}

	//---SCREEN---\\

	private class settransparent extends OneArgFunction {
		public LuaValue call(LuaValue color) {
			console.screen.setTransparent(color.checkint());
			return NIL;
		}
	}

	private class clear extends OneArgFunction {
		public LuaValue call(LuaValue color) {
			console.screen.clear(color.checkint());
			return NIL;
		}
	}

	private class pix extends VarArgFunction {
		public Varargs invoke(Varargs args) {
			int x = args.arg(1).checkint();
			int y = args.arg(2).checkint();
			int color = args.arg(3).checkint();
			int w = args.arg(4).isnil() ? 1 : args.arg(4).checkint();
			int h = args.arg(5).isnil() ? 1 : args.arg(5).checkint();

			console.screen.fill(x, y, x + w - 1, y + h - 1, color);
			return NIL;
		}
	}

	private class write extends VarArgFunction {
		public Varargs invoke(Varargs args) {
			String text = args.arg(1).checkjstring();
			int color = args.arg(2).checkint();
			int x = args.arg(3).checkint();
			int y = args.arg(4).checkint();

			console.screen.write(text, color, x, y);
			return NIL;
		}
	}

	private class spr extends VarArgFunction {
		public Varargs invoke(Varargs args) {
			int id = args.arg(1).checkint();
			int x = args.arg(2).checkint();
			int y = args.arg(3).checkint();

			int scale = args.arg(4).isnil() ? 1 : args.arg(4).checkint();

			int sw = args.arg(5).isnil() ? 1 : args.arg(5).checkint();
			int sh = args.arg(6).isnil() ? 1 : args.arg(6).checkint();

			console.screen.draw(game.getSprite(id, sw, sh).getScaled(scale), x, y);
			return NIL;
		}
	}

	//---MAP---\\

	private class get_tile extends TwoArgFunction {
		public LuaValue call(LuaValue x, LuaValue y) {
			return valueOf(game.map.getTile(x.checkint(), y.checkint()));
		}
	}

	private class set_tile extends ThreeArgFunction {
		public LuaValue call(LuaValue x, LuaValue y, LuaValue id) {
			game.map.setTile(x.checkint(), y.checkint(), id.checkint());
			return NIL;
		}
	}

	private class maprender extends ThreeArgFunction {
		public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
			int scale = arg1.isnil() ? 1 : arg1.checkint();
			int xoff = arg2.isnil() ? 0 : arg2.checkint();
			int yoff = arg3.isnil() ? 0 : arg3.checkint();

			game.map.render(console.screen, game, xoff, yoff, scale);
			return NIL;
		}
	}

}
