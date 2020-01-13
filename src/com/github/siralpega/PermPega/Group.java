package com.github.siralpega.PermPega;

import org.bukkit.ChatColor;

public class Group 
{
	private String name, prefix, suffix;
	private String[] permissions, inheritance;
	private int priority;

	public Group(String name, String[] perms)
	{
		this.name = name;
		this.permissions = perms;
		this.inheritance = null;
		this.priority = 0;
		this.prefix = "[" + name + "]";
		this.suffix = "";
	}

	public Group(String name, String[] perms, String[] inheritanceGroups)
	{
		this.name = name;
		this.permissions = perms;
		this.inheritance = inheritanceGroups;
		this.priority = 0;
		this.prefix = "[" + name + "]";
		this.suffix = "";
	}

	public Group(String name, String[] perms, String[] inheritanceGroups, int priority)
	{
		this.name = name;
		this.permissions = perms;
		this.inheritance = inheritanceGroups;
		this.priority = priority;
		this.prefix = "[" + name + "]";
		this.suffix = "";
	}
	
	public Group(String name, String[] perms, String[] inheritanceGroups, int priority, String prefix)
	{
		this.name = name;
		this.permissions = perms;
		this.inheritance = inheritanceGroups;
		this.priority = priority;
		this.prefix = prefix;
		this.suffix = "";
	}
	
	public Group(String name, String[] perms, String[] inheritanceGroups, int priority, String prefix, String suffix)
	{
		this.name = name;
		this.permissions = perms;
		this.inheritance = inheritanceGroups;
		this.priority = priority;
		this.prefix = ChatColor.translateAlternateColorCodes('&', prefix);
		this.suffix = ChatColor.translateAlternateColorCodes('&', suffix);	
	}

	public String getName()
	{
		return name;
	}

	public String[] getPermissions()
	{
		return permissions;
	}

	public String[] getInheritors()
	{
		return inheritance;
	}

	public boolean hasInheritor(String group)
	{
		for(int i = 0; i < inheritance.length; i++)
			if(inheritance[i].equalsIgnoreCase(group))
				return true;
		return false;
	}

	public int getPriority()
	{
		return priority;
	}
	
	public String getPrefix()
	{
		return prefix;
	}
	
	public String getSuffix()
	{
		return suffix;
	}
}
