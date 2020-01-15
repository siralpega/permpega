package com.github.siralpega.PermPega;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class PermPegaMain extends JavaPlugin implements CommandExecutor, Listener
{
	private File playersFile;
	private YamlConfiguration playersConfig;
	private Map <String, Group> groupData;
	private Map <UUID, PermissionAttachment> playerAttachments;
	private String prefix = ChatColor.DARK_GREEN + "<" + ChatColor.YELLOW + "\\P/" + ChatColor.DARK_GREEN + "> ";
	private static PermPegaMain instance;

	@Override
	public void onEnable()
	{
		instance = this;
		//Commands
		getCommand("permpega");
		getCommand("permpega").setTabCompleter(new CmdTabComplete(this));
	
		//Listeners
		getServer().getPluginManager().registerEvents(this, this);

		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
		playersFile = new File(getDataFolder() + File.separator + "players.yml");
		playersConfig = YamlConfiguration.loadConfiguration(playersFile);
		groupData = new HashMap<String, Group>();
		playerAttachments = new HashMap<UUID, PermissionAttachment>();
		setupGroups();
		//TODO: maybe this a task (runnable) for speed up forced reloads
		for(Player p : Bukkit.getOnlinePlayers())
		{
			playerAttachments.put(p.getUniqueId(), p.addAttachment(this));
			setPlayerPermissions(p, true);
		}
	}

	@Override
	public void onDisable()
	{
		groupData.clear();
		for(Player p : Bukkit.getOnlinePlayers())
			p.removeAttachment(playerAttachments.remove(p.getUniqueId()));
		playerAttachments.clear();
		super.onDisable();
	}

	private void setupGroups()
	{
		for(String group : this.getConfig().getConfigurationSection("groups").getKeys(false))
			groupData.put(group, new Group(group, this.getConfig().getStringList("groups." + group + ".permissions").toArray(new String[0]), 
					this.getConfig().getStringList("groups." + group + ".inherits").toArray(new String[0]), 
					this.getConfig().getInt("groups." + group + ".priority"), this.getConfig().getString("groups." + group + ".prefix"),
					this.getConfig().getString("groups." + group + ".suffix")));
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if(args.length == 0 || args[0].equalsIgnoreCase("help"))
		{
			sender.sendMessage(ChatColor.DARK_GREEN + "/" + label + " add <player> <rank> - " + ChatColor.YELLOW + " adds player to the group");
			sender.sendMessage(ChatColor.DARK_GREEN + "/" + label + " remove <player> <rank> - " + ChatColor.YELLOW + " removes the player from the group");
			sender.sendMessage(ChatColor.DARK_GREEN + "/" + label + " set <player> <rank> - " + ChatColor.YELLOW + " sets player's group to only be <rank>");
			sender.sendMessage(ChatColor.DARK_GREEN + "/" + label + " groups - " + ChatColor.YELLOW + " a list of groups");
			sender.sendMessage(ChatColor.DARK_GREEN + "/" + label + " reload - " + ChatColor.YELLOW + " reloads config and player file");
		}
		else if((args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("set")) && sender.hasPermission("permpega.setrank"))
		{
			if(args.length == 3 && !args[1].isEmpty() && !args[2].isEmpty())
			{
				if(!groupData.containsKey(args[2].toLowerCase()))
					sender.sendMessage(prefix + ChatColor.RED + "No such group '" + args[2] + "'");
				else
				{
					Player player = Bukkit.getPlayerExact(args[1]);
					UUID target;
					if(player != null)
						target = player.getUniqueId();
					else if(player == null && Bukkit.getOfflinePlayer(Bukkit.getOfflinePlayer(args[1]).getUniqueId()).hasPlayedBefore())
						target = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
					else
					{
						sender.sendMessage(prefix + ChatColor.RED + "Player " + ChatColor.AQUA + args[1] + ChatColor.RED + " not found or never joined before.");
						return true;
					}
					if(args[0].equalsIgnoreCase("add"))
					{
						if(!addPlayerToGroup(target, args[2]))
							sender.sendMessage(prefix + ChatColor.DARK_GREEN + args[1] + ChatColor.YELLOW + " is already part of " + ChatColor.DARK_GREEN + args[2].toLowerCase()
									+ ChatColor.YELLOW + " or is part of a group that inherits from " + ChatColor.DARK_GREEN + args[2].toLowerCase() + ChatColor.YELLOW + ". "
									+ "Note: Use " + ChatColor.GREEN + "/pp set <player> <rank> " + ChatColor.YELLOW + " to clear any groups and set the one.");
						else
						{
							if(player != null) //if player is currently online
								setPlayerPermissions(player);
							sender.sendMessage(prefix + ChatColor.YELLOW + "Player " + ChatColor.DARK_GREEN +  args[1] + ChatColor.YELLOW + " was added to group " + ChatColor.DARK_GREEN + args[2].toLowerCase());
							Bukkit.broadcast(prefix + args[1] + ChatColor.YELLOW + " was added to group " + ChatColor.DARK_GREEN + args[2].toLowerCase()
									+ ChatColor.YELLOW + " by " + ChatColor.DARK_GREEN + sender.getName(), "permpega.base");
						}
					}
					else if(args[0].equalsIgnoreCase("remove"))
					{
						if(!removePlayerFromGroup(target, args[2]))
							sender.sendMessage(prefix + ChatColor.DARK_GREEN + args[1] + ChatColor.YELLOW + " is not in group " + ChatColor.DARK_GREEN + args[2].toLowerCase());
						else
						{
							if(player != null) //if player is currently online
								setPlayerPermissions(player, true);
							sender.sendMessage(prefix + ChatColor.YELLOW + "Player " + ChatColor.DARK_GREEN +  args[1] + ChatColor.YELLOW + " was removed from group " + ChatColor.DARK_GREEN + args[2].toLowerCase());
							Bukkit.broadcast(prefix + args[1] + ChatColor.YELLOW + " was removed from group " + ChatColor.DARK_GREEN + args[2].toLowerCase()
									+ ChatColor.YELLOW + " by " + ChatColor.DARK_GREEN + sender.getName(), "permpega.base");
						}
					}
					else if(args[0].equalsIgnoreCase("set"))
					{
						String[] groupsToRemove = getPlayerGroups(target);
						boolean success = true;
						for(String group : groupsToRemove)
							if(!removePlayerFromGroup(target, group))
								success = false;

						if(!addPlayerToGroup(target, args[2]) && !success)
						{
							sender.sendMessage(prefix + ChatColor.RED + "[!] ERROR: Could set group " + ChatColor.YELLOW + args[2] + ChatColor.RED + " to player " + ChatColor.YELLOW + args[1] + ChatColor.RED + ". Tell an admin");
							return true;
						}
						if(!success)
						{
							sender.sendMessage(prefix + ChatColor.RED + "[!] ERROR: Could not clear " + ChatColor.YELLOW + args[1] + ChatColor.RED + "'s groups, but was able to add them to " + ChatColor.YELLOW + args[2]);
							return true;
						}
						if(player != null) //if player is currently online
							setPlayerPermissions(player, true);
						sender.sendMessage(prefix + ChatColor.YELLOW + "Player " + ChatColor.DARK_GREEN +  args[1] + ChatColor.YELLOW + " had their group set to " + ChatColor.DARK_GREEN + args[2].toLowerCase());
						Bukkit.broadcast(prefix + args[1] + ChatColor.YELLOW + " had their group set to " + ChatColor.DARK_GREEN + args[2].toLowerCase()
								+ ChatColor.YELLOW + " by " + ChatColor.DARK_GREEN + sender.getName(), "permpega.base");
					}
				}
			}
			else
				sender.sendMessage(prefix + ChatColor.RED + "Incorrect syntax or unknown sub-command " + ChatColor.GREEN + "/" + label + " help");
		}
		else if(args[0].equalsIgnoreCase("reload") && sender.hasPermission("permpega.reload"))
			reload();
		else if(args[0].equalsIgnoreCase("groups") && sender.hasPermission("permpega.setrank"))
		{
			String groups = "";
			for(String group : this.getConfig().getStringList("groups"))
				groups += group + " ";
			sender.sendMessage(prefix + "Groups: " + ChatColor.YELLOW + groups);
		}
		else
			sender.sendMessage(prefix + ChatColor.RED + "Incorrect syntax.");
		return true;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event)
	{
		//TODO: for default: if getPlayerGroups is null or length == 0, then don't add attachment or set permissions.
		playerAttachments.put(event.getPlayer().getUniqueId(), event.getPlayer().addAttachment(this));
		setPlayerPermissions(event.getPlayer());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event)
	{
		PermissionAttachment attach = playerAttachments.remove(event.getPlayer().getUniqueId());
		event.getPlayer().removeAttachment(attach);
	}

	private void reload()
	{
		try 
		{
			playersConfig.load(playersFile);
			this.reloadConfig();
			setupGroups();
			for(Player p : Bukkit.getOnlinePlayers())
				setPlayerPermissions(p, true);
			getLogger().info("Reloaded configs");
		} catch (IOException | InvalidConfigurationException e) 
		{
			getLogger().warning("COULD NOT RELOAD players.yml.");
			e.printStackTrace();
		}
	}

	private void setPlayerPermissions(Player p)
	{
		setPlayerPermissions(p, false);
	}
	private void setPlayerPermissions(Player p, boolean clear) 
	{
		if(clear)
		{
			PermissionAttachment attach = playerAttachments.remove(p.getUniqueId());
			p.removeAttachment(attach);
			playerAttachments.put(p.getUniqueId(), p.addAttachment(this));
		}
		PermissionAttachment attach = playerAttachments.get(p.getUniqueId());
		String[] playerGroups = getPlayerGroups(p.getUniqueId());
		if(playerGroups.length == 0)
		{
			p.setDisplayName(ChatColor.RESET + p.getName());
			return; //try and make it so a default player has no group. certain commands will not check for permission / always run, no matter if permission set.
		}
		int highestPriority = -1;
		String prefix = "", suffix = "";
		for(String group : playerGroups)
		{
			//Attach permissions from the player's group(s)
			for(String permission : groupData.get(group).getPermissions())
			{
				attach.setPermission(permission, true); 
				if(groupData.get(group).getPriority() > highestPriority)
				{
					highestPriority = groupData.get(group).getPriority();
					prefix = groupData.get(group).getPrefix();
					suffix = groupData.get(group).getSuffix();
				}
			}
			//Attach permissions from the player's group's inheritor(s)
			for(String parent : groupData.get(group).getInheritors())
				for(String parentPermission : groupData.get(parent).getPermissions())
					attach.setPermission(parentPermission, true); 
		}
		p.setDisplayName(prefix + p.getName() + suffix);
	}

	private String[] getPlayerGroups(UUID id)
	{
		//config.load(players);
		String fromConfig = playersConfig.getString(id.toString());
		if(fromConfig == null || fromConfig.isEmpty())
			return new String[0];
		//	fromConfig = fromConfig.replace("[", "").replace("]", "");
		return fromConfig.split(",");
	}

	private boolean addPlayerToGroup(UUID id, String group)
	{
		try
		{
			playersConfig.load(playersFile);
			String[] groups = getPlayerGroups(id);
			for(int i = 0; i < groups.length; i++)
				if(groups[i].equalsIgnoreCase(group) || groupData.get(groups[i]).hasInheritor(group)) //duplicate or already inheriting group from a current player group
					return false;
			String toConfig = "";// = "[";
			if(groups.length == 0)
				toConfig += group.toLowerCase(); // + "]";
			else
			{
				List<Integer> dontAddIndex = new ArrayList<Integer>();
				//If our new group inherits one of our current groups, remove the current group from our new list of groups
				for(int i = 0; i < groups.length; i++)
					if(groupData.get(group).hasInheritor(groups[i]))
						dontAddIndex.add(i);
				//Add our groups, including the new group and excluding groups that are inherited by the new group, to our id string in players file
				for(int i = 0; i < groups.length; i++)
				{
					if(dontAddIndex.contains(i))
						continue;
					else if(toConfig.isEmpty())
						toConfig += groups[i];
					else
						toConfig += ", " + groups[i];
				}
				toConfig += "," + group.toLowerCase(); // + "]";
				if(toConfig.startsWith(","))
					toConfig = toConfig.substring(1);
			}
			playersConfig.set(id.toString(), toConfig);
			playersConfig.save(playersFile);
		}
		catch (IOException | InvalidConfigurationException e) 
		{
			getLogger().warning("FILE EXCEPTION: Could not set group for " + Bukkit.getOfflinePlayer(id).getName());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private boolean removePlayerFromGroup(UUID id, String group)
	{
		try 
		{
			playersConfig.load(playersFile);
			String[] groups = getPlayerGroups(id);
			int i;
			for(i = 0; i < groups.length; i++)
				if(groups[i].equalsIgnoreCase(group))
					break;
			if(i == groups.length || groups.length == 0)
				return false; //player not in group 
			String toConfig = ""; //"[";
			if(i != 0)
				toConfig += groups[0];
			for(int x = 1; x < groups.length; x++)
				if(x != i)
					toConfig += ", " + groups[x];
			//		toConfig += "]";
			if(toConfig.startsWith(","))
				toConfig = toConfig.substring(toConfig.indexOf(",") + 2);
			playersConfig.set(id.toString(), toConfig);
			playersConfig.save(playersFile);
		}
		catch (IOException | InvalidConfigurationException e) 
		{
			getLogger().warning("FILE EXCEPTION: Could not set group for " + Bukkit.getOfflinePlayer(id).getName());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static PermPegaMain getInstance() 
	{
		return instance;
	}

	public Map<String, Group> getGroups()
	{
		return groupData;
	}
}
