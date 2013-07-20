package ca.gibstick.discosheep;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

public class DiscoSheepCommandExecutor implements CommandExecutor {

	private DiscoSheep parent;

	public DiscoSheepCommandExecutor(DiscoSheep parent) {
		this.parent = parent;
	}
	private static final String PERMISSION_PARTY = "discosheep.party";
	private static final String PERMISSION_ALL = "discosheep.partyall";
	private static final String PERMISSION_FIREWORKS = "discosheep.fireworks";
	private static final String PERMISSION_STOPALL = "discosheep.stopall";
	private static final String PERMISSION_RELOAD = "discosheep.reload";
	private static final String PERMISSION_OTHER = "discosheep.partyother";
	private static final String PERMISSION_CHANGEPERIOD = "discosheep.changeperiod";

	//private static final String DELIM = "[ ]+";
	private boolean senderHasPerm(CommandSender sender, String permission) {
		return sender.hasPermission(permission);
	}

	private boolean noPermsMessage(CommandSender sender, String permission) {
		sender.sendMessage(ChatColor.RED + "You do not have the permission node " + ChatColor.GRAY + permission);
		return false;
	}

	private boolean parseNextArg(String[] args, int i, String compare) {
		if (i < args.length - 1) {
			return args[i + 1].equalsIgnoreCase(compare);
		}
		return false;
	}

	private int parseNextIntArg(String[] args, int i) {
		if (i < args.length - 1) {
			try {
				return Integer.parseInt(args[i + 1]);
			} catch (NumberFormatException e) {
				return -1;
			}
		}
		return -1;
	}

	private Double parseNextDoubleArg(String[] args, int i) {
		if (i < args.length - 1) {
			return Double.parseDouble(args[i + 1]);
		}
		return -1.0d;
	}

	// extract a list of players from a list of arguments
	private String[] parsePlayerList(String[] args, int i) {
		int j = i;
		while (j < args.length && !args[j].startsWith("-")) {
			j++;
		}
		return Arrays.copyOfRange(args, i, j);
	}

	/*-- Actual commands begin here --*/
	private boolean helpCommand(CommandSender sender) {
		sender.sendMessage(ChatColor.YELLOW + "DiscoSheep Help\n"
				+ ChatColor.GRAY + "  Subcommands\n" + ChatColor.WHITE
				+ "me: start a party for yourself\n"
				+ "stop: stop your own party\n"
				+ "all: start a party for all players on the server\n"
				+ "stopall: stop all parties (takes no arguments)\n"
				+ "other <players>: start a party for the space-delimited list of players\n"
				+ ChatColor.GRAY + "  Arguments\n" + ChatColor.WHITE
				+ "-n <integer>: set the number of sheep per player that spawn\n"
				+ "-t <integer>: set the party duration in seconds\n"
				+ "-p <ticks>: set the number of ticks between each disco beat\n"
				+ "-r <integer>: set radius of the area in which sheep can spawn\n"
				+ "-fw: enables fireworks");
		return true;
	}

	private boolean reloadCommand(CommandSender sender) {
		if (senderHasPerm(sender, PERMISSION_RELOAD)) {
			parent.reloadConfigFromDisk();
			sender.sendMessage(ChatColor.GREEN + "DiscoSheep config reloaded from disk");
			return true;
		} else {
			return noPermsMessage(sender, PERMISSION_RELOAD);
		}
	}

	private boolean partyCommand(Player player, DiscoParty party) {
		if (senderHasPerm(player, PERMISSION_PARTY)) {
			if (!parent.hasParty(player.getName())) {
				party.setPlayer(player);
				party.startDisco();
			} else {
				player.sendMessage("You already have a party. Are you underground?");
			}
			return true;
		} else {
			return noPermsMessage(player, PERMISSION_PARTY);
		}
	}

	private boolean partyAllCommand(CommandSender sender, DiscoParty party) {
		if (senderHasPerm(sender, PERMISSION_ALL)) {
			for (Player p : Bukkit.getServer().getOnlinePlayers()) {
				if (!parent.hasParty(p.getName())) {
					DiscoParty individualParty = party.DiscoParty(p);
					individualParty.startDisco();
					p.sendMessage(ChatColor.RED + "LET'S DISCO!");
				}
			}
			return true;
		} else {
			return noPermsMessage(sender, PERMISSION_ALL);
		}
	}

	private boolean stopAllCommand(CommandSender sender) {
		if (senderHasPerm(sender, PERMISSION_STOPALL)) {
			parent.stopAllParties();
			return true;
		} else {
			return noPermsMessage(sender, PERMISSION_STOPALL);
		}
	}

	private boolean stopMeCommand(CommandSender sender) {
		parent.stopParty(sender.getName());
		return true;
	}

	private boolean partyOtherCommand(String[] players, CommandSender sender, DiscoParty party) {
		if (senderHasPerm(sender, PERMISSION_OTHER)) {
			Player p;
			for (String playerName : players) {
				p = Bukkit.getServer().getPlayer(playerName);
				if (p != null) {
					if (!parent.hasParty(p.getName())) {
						DiscoParty individualParty = party.DiscoParty(p);
						individualParty.startDisco();
					}
				} else {
					sender.sendMessage("Invalid player: " + playerName);
				}
			}
			return true;
		} else {
			return noPermsMessage(sender, PERMISSION_OTHER);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		Player player = null;
		boolean isPlayer = false;

		if (sender instanceof Player) {
			player = (Player) sender;
			isPlayer = true;
		}

		// check for commands that don't need a party
		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("stopall")) {
				return stopAllCommand(sender);
			} else if (args[0].equalsIgnoreCase("stop") && isPlayer) {
				return stopMeCommand(sender);
			} else if (args[0].equalsIgnoreCase("help")) {
				return helpCommand(sender);
			} else if (args[0].equalsIgnoreCase("reload")) {
				return reloadCommand(sender);
			}
		}

		DiscoParty parentParty = new DiscoParty(parent);

		for (int i = 1; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-fw")) {
				if (senderHasPerm(sender, PERMISSION_FIREWORKS)) {
					parentParty.setDoFireworks(true);
				} else {
					return noPermsMessage(sender, PERMISSION_FIREWORKS);
				}
			} else if (args[i].equalsIgnoreCase("-r")) {
				try {
					parentParty.setRadius(parseNextIntArg(args, i));
					//sender.sendMessage("RADIUS OK");
				} catch (IllegalArgumentException e) {
					sender.sendMessage("Radius must be an integer within the range [1, "
							+ DiscoParty.maxRadius + "]");
					return false;
				}
			} else if (args[i].equalsIgnoreCase("-n")) {
				try {
					parentParty.setSheep(parseNextIntArg(args, i));
					//sender.sendMessage("SHEEP OK");
				} catch (IllegalArgumentException e) {
					sender.sendMessage("The number of sheep must be an integer within the range [1, "
							+ DiscoParty.maxSheep + "]");
					return false;
				}
			} else if (args[i].equalsIgnoreCase("-t")) {
				try {
					parentParty.setDuration(parent.toTicks(parseNextIntArg(args, i)));
					//sender.sendMessage("DURATION OK");
				} catch (IllegalArgumentException e) {
					sender.sendMessage("The duration in seconds must be an integer within the range [1, "
							+ parent.toSeconds(DiscoParty.maxDuration) + "]");
					return false;
				}
			} else if (args[i].equalsIgnoreCase("-p")) {
				if (!senderHasPerm(sender, PERMISSION_CHANGEPERIOD)) {
					return noPermsMessage(sender, PERMISSION_CHANGEPERIOD);
				}
				try {
					parentParty.setPeriod(parseNextIntArg(args, i));
					//sender.sendMessage("PERIOD OK");
				} catch (IllegalArgumentException e) {
					sender.sendMessage(
							"The period in ticks must be within the range ["
							+ DiscoParty.minPeriod + ", "
							+ DiscoParty.maxPeriod + "]");
					return false;
				}
			}
		}

		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("all")) {
				return partyAllCommand(sender, parentParty);
			} else if (args[0].equalsIgnoreCase("me") && isPlayer) {
				return partyCommand(player, parentParty);
			} else if (args[0].equalsIgnoreCase("other")) {
				return partyOtherCommand(parsePlayerList(args, 1), sender, parentParty);
			} else {
				sender.sendMessage(ChatColor.RED + "Invalid argument (certain commands do not work from console).");
				return false;
			}
		}

		return false;
	}
}