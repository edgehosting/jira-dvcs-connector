package com.atlassian.jira.plugins.bitbucket.api;

public enum DvcsTypeEnum
{

	BITBUCKET("bitbucket"), GITHUB("github");

	private String type;

	public String getType()
	{
		return type;
	}

	private DvcsTypeEnum(String type)
	{
		this.type = type;
	}

	public DvcsTypeEnum toEnum(String type)
	{
		DvcsTypeEnum[] values = values();
		for (DvcsTypeEnum dvcsTypeEnum : values)
		{
			if (dvcsTypeEnum.getType().equalsIgnoreCase(type))
			{
				return dvcsTypeEnum;
			}
		}
		throw new IllegalArgumentException("Can't convert given value to enum: " + type);
	}

}
