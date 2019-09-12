package com.company;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.*;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfLoginInfo;

import java.io.*;
import java.util.*;

class Helper {
    private IDfSessionManager sessionManager;
    private HashMap<String, String> map;
    IDfSession session;
    IDfQuery query;
    public Properties prop;
    public static class Constants {
        public static final String GET_CONTENT_TYPE_DOS_EXTENSION = "select distinct a_content_type, dos_extension from dm_document,dm_format where a_content_type = name;";

        public static final String CONFIG_FILE_NAME =               "app.properties";
        public static final String APPLICATION_MODE =               "app.mode";
        public static final String PERMISSION_SET_NAME =            "app.permission_set_name";
        public static final String EXPORT_FILE_NAME =               "app.export_file_name";
        public static final String PERMISSION_SETS_FILE_NAME =      "app.permission_sets_file_path";
        public static final String OBJECT_LIST_FILE_PATH =          "app.object_list_file_path";

        public static final String DOCBROKER_HOST =                 "dfc.docbroker.host[0]";
        public static final String DOCBROKER_PORT =                 "dfc.docbroker.port[0]";
        public static final String REPOSITORY_NAME =                "dfc.globalregistry.repository";
        public static final String USERNAME =                       "dfc.username";
        public static final String PASSWORD =                       "dfc.password";
        //Constants() { }
    } Constants constants;
    //----------------------------------------------------------------------------------------------------------------------
    public Helper() {
        //if(readConfig(constants.CONFIG_FILE_NAME))  {
            query = new DfQuery();
        //}
//        else                                        { System.out.println("Failed to read config file.");}
    }
    //----------------------------------------------------------------------------------------------------------------------
    public void mapExtensions(IDfSession session) throws DfException {
        map = new HashMap<>();
        query.setDQL(constants.GET_CONTENT_TYPE_DOS_EXTENSION);
        IDfCollection coll = query.execute(session, IDfQuery.DF_READ_QUERY);
        while (coll.next()) {
            System.out.println(coll.getString("a_content_type") + " " + coll.getString("dos_extension"));
            map.put(coll.getString("a_content_type"),coll.getString("dos_extension"));
        }
        if (coll != null)
            coll.close();
        if(map.isEmpty()) throw new DfException("Mapper is empty");
    }
    //----------------------------------------------------------------------------------------------------------------------
    String getExtension(String objectType) throws DfException {
        if(map == null) { mapExtensions(this.session); }
        return map.get(objectType); }
    //----------------------------------------------------------------------------------------------------------------------
    public boolean connect() {
        IDfClientX clientx = new DfClientX();
        IDfClient client;
        try {
            client = clientx.getLocalClient();
            IDfLoginInfo loginInfo = clientx.getLoginInfo();
            sessionManager = client.newSessionManager();
            loginInfo.setUser(prop.getProperty(constants.USERNAME));
            loginInfo.setPassword(prop.getProperty(constants.PASSWORD));
            sessionManager.setIdentity(prop.getProperty(constants.REPOSITORY_NAME),loginInfo);
            session = sessionManager.getSession(prop.getProperty(constants.REPOSITORY_NAME));
            return true;
        } catch (DfException ex) {
            ex.printStackTrace();
            ex.getMessage();
            return false;
        }
    }
    //----------------------------------------------------------------------------------------------------------------------
    public void disconnect() {
        sessionManager.release(session);
    }
    //----------------------------------------------------------------------------------------------------------------------
    public boolean readConfig(String configFileName) {
        prop = new Properties();
        InputStream is;
        try {
            is = new FileInputStream(configFileName);
        } catch (FileNotFoundException ex) { System.out.println(ex.getMessage());   return false; }

        BufferedReader br = new BufferedReader( new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            prop.load(new StringReader(sb.toString().replace("\\", "\\\\")));

        } catch (IOException ex) {
            return false;
        }
        prop.forEach((key, value) -> { prop.setProperty(key.toString(),value.toString().substring(0,(value.toString().indexOf("#") > 0 ? value.toString().indexOf("#") : value.toString().length())).trim()); }); // removing comments
        prop.forEach((key, value) -> { System.out.println(key.toString() +": "+value.toString()); });
        return true;
    }
    //----------------------------------------------------------------------------------------------------------------------
}

