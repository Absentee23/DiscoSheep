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
				return -1; // so that it fails limit checks elsewhere
			}
		}
		return -1; // ibid
	}

	private Double parseNextDoubleArg(String[] args, int i) {
		if (i < args.length - 1) {
			try {
				return Double.parseDouble(args[i + 1]);
			} catch (NumberFormatException e) {
				return -1.0d; // so that it fais limit checks elsewhere
			}
		}
		return -1.0d; // ibid
	}

	// return portion of the array that contains the list of players
	private String[] parsePlayerList(String[] args, int i) {
		int j = i;
		while (j < args.length && !args[j].startsWith("-")) {
			j++;
		}
		return Arrays.copyOfRange(args, i, j);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		Player player = null;
		boolean isPlayer = false;
		boolean specialRadius = false;
		// flag to determine if we calculate a radius so that the sheep spawn densely in an area

		if (sender instanceof Player) {
			player = (Player) sender;
			isPlayer = true;
		} // check isPlayer before "stop" and "me" commands

		// check for commands that don't need a party
		// so that we get them out of the way, and 
		// prevent needless construction of parties
		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("stopall")) {
				return parent.stopAllCommand(sender, this);
			} else if (args[0].equalsIgnoreCase("stop") && isPlayer) {
				return parent.stopMeCommand(sender, this);
			} else if (args[0].equalsIgnoreCase("help")) {
				return parent.helpCommand(sender);
			} else if (args[0].equalsIgnoreCase("reload")) {
				return parent.reloadCommand(sender, this);
			}
		}

		// construct a main party; all other parties will copy from this
		DiscoParty mainParty = new DiscoParty(parent);

		// omg I love argument parsing and I know the best way!
		for (int i = 1; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-fw")) {
				if (sender.hasPermission(DiscoSheep.PERMISSION_FIREWORKS)) {
					mainParty.setDoFireworks(true);
				} else {
					return parent.noPermsMessage(sender, DiscoSheep.PERMISSION_FIREWORKS);
				}
			} else if (args[i].equalsIgnoreCase("-r")) {
				if (parseNextArg(args, i, "dense")) {
					specialRadius = true;
				}
				if (!specialRadius) {
					try {
						mainParty.setRadius(parseNextIntArg(args, i));
					} catch (IllegalArgumentException e) {
						sender.sendMessage("Radius must be an integer within the range [1, "
								+ DiscoParty.maxRadius + "]");
						return false;
					}
				}
			} else if (args[i].equalsIgnoreCase("-n")) {
				try {
					mainParty.setSheep(parseNextIntArg(args, i));
				} catch (IllegalArgumentException e) {
					sender.sendMessage("The number of sheep must be an integer within the range [1, "
							+ DiscoParty.maxSheep + "]");
					return false;
				}
			} else if (args[i].equalsIgnoreCase("-t")) {
				try {
					mainParty.setDuration(parent.toTicks(parseNextIntArg(args, i)));
				} catch (IllegalArgumentException e) {
					sender.sendMessage("The duration in seconds must be an integer within the range [1, "
							+ parent.toSeconds(DiscoParty.maxDuration) + "]");
					return false;
				}
			} else if (args[i].equalsIgnoreCase("-p")) {
				if (!sender.hasPermission(DiscoSheep.PERMISSION_CHANGEPERIOD)) {
					return parent.noPermsMessage(sender, DiscoSheep.PERMISSION_CHANGEPERIOD);
				}
				try {
					mainParty.setPeriod(parseNextIntArg(args, i));
				} catch (IllegalArgumentException e) {
					sender.sendMessage(
							"The period in ticks must be within the range ["
							+ DiscoParty.minPeriod + ", "
							+ DiscoParty.maxPeriod + "]");
					return false;
				}
			}
		}

		if (specialRadius) {
			mainParty.setDenseRadius(mainParty.getSheep());
		}

		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("all")) {
				return parent.partyAllCommand(sender, mainParty, this);
			} else if (args[0].equalsIgnoreCase("me") && isPlayer) {
				return parent.partyCommand(player, mainParty, this);
			} else if (args[0].equalsIgnoreCase("other")) {
				return parent.partyOtherCommand(parsePlayerList(args, 1), sender, mainParty, this);
			} else {
				sender.sendMessage(ChatColor.RED + "Invalid argument (certain commands do not work from console).");
				return false;
			}
		}

		return false;
	}
}
