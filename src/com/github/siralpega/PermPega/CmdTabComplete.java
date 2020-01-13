package com.github.siralpega.PermPega;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class CmdTabComplete implements TabCompleter
{
	private PermPegaMain plugin;
	public CmdTabComplete(PermPegaMain main) {
		plugin = main;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) 
	{
		List<String> options = new ArrayList<String>();
		if(args.length == 1)
		{
			options.add("add");
			options.add("remove");
			options.add("set");
			options.add("reload");
			options.add("help");
			if(!args[0].isEmpty())
			{
				for(int i = 0; i < options.size(); i++)
					if(!options.get(i).startsWith(args[0]))
						options.remove(i);
			}
		}
		else if(args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || (args[0].equalsIgnoreCase("set"))))
		{
			Iterator<? extends Player> it = Bukkit.getOnlinePlayers().iterator();
			while(it.hasNext())
				options.add(it.next().getName());
			if(!args[1].isEmpty())
			{
				for(int i = 0; i < options.size(); i++)
					if(!options.get(i).startsWith(args[1]))
						options.remove(i);
			}
		}
		else if(args.length == 3 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || (args[0].equalsIgnoreCase("set"))))
		{
			Iterator<Map.Entry<String,Group>> it = plugin.getGroups().entrySet().iterator();
			while(it.hasNext())
				options.add(it.next().getKey());

			if(!args[2].isEmpty())
			{
				for(int i = 0; i < options.size(); i++)
					if(!options.get(i).startsWith(args[2]))
						options.remove(i);
			}
		}
		return options;
	}

}
