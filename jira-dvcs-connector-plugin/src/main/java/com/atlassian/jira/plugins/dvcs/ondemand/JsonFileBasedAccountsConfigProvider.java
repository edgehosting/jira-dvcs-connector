package com.atlassian.jira.plugins.dvcs.ondemand;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class JsonFileBasedAccountsConfigProvider implements AccountsConfigProvider
{

    private static Logger log = LoggerFactory.getLogger(JsonFileBasedAccountsConfigProvider.class);

    private String absoluteConfigFilePath = "/data/jirastudio/home/ondemand.properties";

    public JsonFileBasedAccountsConfigProvider()
    {
        super();
    }

    @Override
    public AccountsConfig provideConfiguration()
    {
        File configFile = new File(absoluteConfigFilePath);

        try
        {
            AccountsConfig config = null;


            GsonBuilder builder = new GsonBuilder();
            builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES);
            Gson gson = builder.create();
            
            config = gson.fromJson(new InputStreamReader(new FileInputStream(configFile)), AccountsConfig.class);
            
            return config;
       
        } catch (JsonSyntaxException e)
        {
            throw new IllegalStateException("Failed to parse file: " + configFile);
            
        } catch (JsonIOException e)
        {
            throw new IllegalStateException("Failed to read file: " + configFile);
            
        } catch (FileNotFoundException e)
        {
            throw new IllegalStateException("File not found: " + configFile);
        }
    }

    @Override
    public boolean supportsIntegratedAccounts()
    {
        File configFile = new File(absoluteConfigFilePath);

        if (configFile.exists())
        {

            if (!configFile.canRead())
            {

                log.error(configFile + " can not be red.");

                throw new IllegalStateException(configFile + " can not be red.");

            } else
            {

                return true;

            }

        }

        return false;
    }

}
