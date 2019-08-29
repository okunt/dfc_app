package com.company;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.*;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfLoginInfo;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RepoApp {

    private IDfSession session = null;
    private IDfSessionManager sessionManager = null;
    private Properties prop = null;
    private ArrayList<String> rObjectId = new ArrayList<>();
    private TypeToExtensionMapper mapper = null;
    //----------------------------------------------------------------------------------------------------------------------
    public void connect() {
        IDfClientX clientx = new DfClientX();
        IDfClient client;

        try {
            client = clientx.getLocalClient();
            IDfLoginInfo loginInfo = clientx.getLoginInfo();
            sessionManager = client.newSessionManager();
            loginInfo.setUser("dmadmin");//(Variables.USERNAME);
            loginInfo.setPassword("password");//(Variables.PASSWORD);
            sessionManager.setIdentity("idm_dev",loginInfo);//(Variables.REPOSITORY, loginInfo);
            session = sessionManager.getSession("idm_dev");//(Variables.REPOSITORY);

        } catch (DfException ex) {
            ex.printStackTrace();
        }
    }
    //----------------------------------------------------------------------------------------------------------------------
    public void disconnect() {
        sessionManager.release(session);
    }
    //----------------------------------------------------------------------------------------------------------------------
    RepoApp() {
        connect();
        if(readConfig())   invokeFunction();
        disconnect();
//    connect();
//        try {
//            typeDemo("dm_acl");
//            //typeDemo("dm_document");
//            //typeAttributesDemo("dm_acl");
//            //typeDumpDemo("dm_acl");
//            //selectDocumentsForStudyDemo("ML00780");
//            //documentModificationDemo("090f42df8025afbc");
//            //apiDemo();
//        } catch (DfException ex) {
//            System.out.println(ex.toString());
//            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//         }
//  //  disconnect();
    }
    //----------------------------------------------------------------------------------------------------------------------
    public boolean readConfig() {
        prop = new Properties();
        String fileName = "app.properties";
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
        } catch (FileNotFoundException ex) {
            return false;
        }
        try {
            BufferedReader br = new BufferedReader( new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            prop.load(new StringReader(sb.toString().replace("\\", "\\\\")));

        } catch (IOException ex) {
            return false;
        }
        prop.forEach((key, value) -> { prop.setProperty(key.toString(),value.toString().substring(0,value.toString().indexOf("#")).trim()); }); // removing comments
        prop.forEach((key, value) -> { System.out.println(key.toString() +": "+value.toString()); });
        return true;
    }
    //----------------------------------------------------------------------------------------------------------------------
    public boolean invokeFunction() {

        try {
            switch (prop.getProperty("mode")) {
                case "content_export":
                        mapper = new TypeToExtensionMapper(session);    return contentExport();
                case "properties_export":                               return propertiesExport();
                case "permission_set_assignment":                       return permissionAssignment();
                case "permission_sets_creation":                        return permissionsCreation();
                default:                                                return false;
            }
        } catch (DfException e) {
            e.printStackTrace();
            return false;
        }
    }
    //----------------------------------------------------------------------------------------------------------------------
    private boolean contentExport() {
        if(setObjectsIds()) {
//            if(prop.getProperty("export_file_path").isEmpty())
//                return false;
//            else {
                while(!rObjectId.isEmpty()) {
                    objectContentExport(rObjectId.remove(0),prop.getProperty("export_file_path"));
                }
                return true;
           // }
        }
        System.out.println("setObjectsIds failed!");
        return false;
    }
    //----------------------------------------------------------------------------------------------------------------------
    private boolean objectContentExport(String objectId, String exportPath) {   // docx, xlsx oraz pptx. Należy stworzyć klasę odpowiedzialną za mapowanie typów na rozszerzenia plików
        IDfSysObject sysObj;
        ByteArrayInputStream data;
        String objectName = "";
        String contentType;
        try {
            sysObj = (IDfSysObject) session.getObject(new DfId(objectId));
            contentType = sysObj.getContentType();
            objectName = sysObj.getObjectName().replace('/',' ');
            data = sysObj.getContent();
        } catch (DfException e) {
            e.printStackTrace();
            return false;
        }

        OutputStream out;
        try {
            out = new FileOutputStream(exportPath + objectName + "." + mapper.getExtension(contentType));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        byte[] buf = new byte[1024];
        int len;
        while (true) {
            try {
                if (!((len = data.read(buf)) > 0)) break;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            try {
                out.write(buf, 0, len);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        try {
            data.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    //----------------------------------------------------------------------------------------------------------------------
    private boolean propertiesExport() throws DfException {
        if(setObjectsIds()) {
            if(prop.getProperty("export_file_name").isEmpty())
                return false;
            else {
                String excelFileName = prop.get("export_file_name").toString();
                if (!excelFileName.contains(".xlsx")) excelFileName += ".xlsx";
                Workbook wb = new XSSFWorkbook();
                Sheet sheet = wb.createSheet("Properties");
                IDfQuery query = new DfQuery();
                IDfCollection coll;
                boolean firstQuery = true;
                Vector<String> propertyName = new Vector<>();
                int rowNumber = 0, cellNumber = 0, columnCount = 0;
                while(!rObjectId.isEmpty()) {   // objectPropertiesExport(rObjectId.remove(0));
                    String dql = "select * from dm_document where r_object_id = '" + rObjectId.remove(0) + "';";
                    query.setDQL(dql);
                    coll = query.execute(session, IDfQuery.DF_READ_QUERY);
                    columnCount = coll.getAttrCount();
                    Row row;
                    if(firstQuery) {
                        firstQuery = false;
                        row = sheet.createRow((short) rowNumber++);
                        for (int i = 0; i < columnCount; i++) {
                            Cell cell = row.createCell(cellNumber++);
                            cell.setCellValue(coll.getAttr(i).getName());
                            propertyName.add(coll.getAttr(i).getName());
                        }
                    }
                    while (coll.next()) {
                        cellNumber = 0;
                        row = sheet.createRow((short) rowNumber++);
                        for (int i = 0; i < columnCount; i++) {
                            Cell cell = row.createCell(cellNumber++);
                            cell.setCellValue(coll.getString(propertyName.elementAt(i)));
                        }
                    }
                }

                try {
                    FileOutputStream fos = new FileOutputStream(excelFileName);
                    wb.write(fos);
                    fos.flush();
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        }
        return false;
    }
    //----------------------------------------------------------------------------------------------------------------------
    private boolean permissionAssignment() throws DfException {
        if(setObjectsIds()) {
            if(prop.getProperty("permission_set_name").isEmpty())
                return false;
            else {
                String psName = prop.getProperty("permission_set_name");

                String dql = "select owner_name from dm_acl where object_name = '"+ psName +"' enable(return_top 1);";
                IDfQuery query = new DfQuery();
                query.setDQL(dql);
                IDfCollection coll = query.execute(session, IDfQuery.DF_READ_QUERY);
                String psDomain = "";
                if(coll.next()) {
                    psDomain = coll.getString("owner_name");
                }

                if (coll != null)
                    coll.close();

                while(!rObjectId.isEmpty()) {
                    dql = "update dm_document objects " +
                            "SET acl_domain = '"+ psDomain +"' SET acl_name = '" + psName + "'" +
                            "where r_object_id = '"+rObjectId.remove(0)+"';";
                    query.setDQL(dql);
                    query.execute(session,IDfQuery.EXEC_QUERY);
                }
                return true;
            }
        }
        return false;
    }
    //----------------------------------------------------------------------------------------------------------------------
    private boolean permissionsCreation() throws DfException {
        String fileName = prop.getProperty("permission_sets_file_path");
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
        } catch (FileNotFoundException ex) {
            return false;
        }
        try {
            BufferedReader br = new BufferedReader( new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                String[] params = line.split(";");
                if(params.length<6) continue;
                int i = 5;
                IDfPersistentObject object = (IDfPersistentObject)session.newObject("dm_acl");
                object.apiSet("set", "owner_name", params[2]);
                object.apiSet("set", "description", params[1]);
                object.apiSet("set", "object_name", params[0]);
                object.apiExec("grant", params[3]+","+params[4]+","+params[5]);
                while(i+3<params.length) { object.apiExec("grant", params[i+1]+","+params[i+2]+","+params[i+3]); i += 3; };
                object.save();
            }
        } catch (IOException ex) {
            return false;
        }
        return true;
    }
    //----------------------------------------------------------------------------------------------------------------------
    private boolean setObjectsIds() {
        String fileName = prop.getProperty("object_list_file_path");
        if(fileName.isEmpty())
            return false;
        InputStream is = null;
        BufferedReader reader = null;

        try {
            is = new FileInputStream(fileName);
            reader = new BufferedReader(new InputStreamReader(is));

            String line = reader.readLine();
            while (line != null) {
                this.rObjectId.add(line);
                System.out.println(line);
                line = reader.readLine();
            }
            if(rObjectId.isEmpty()) return false;
            else return true;
        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException ex) {
            Logger.getLogger(RepoApp.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } finally {
            try {
                reader.close();
                is.close();
            } catch (IOException ex) {
                Logger.getLogger(RepoApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    //----------------------------------------------------------------------------------------------------------------------
    public void typeDemo(String typeName) throws DfException {
        IDfType type = session.getType(typeName);
        System.out.println("Info about " + typeName + " type:");
        System.out.println("Name: " + type.getName());
        System.out.println("Description: " + type.getDescription());
        System.out.println("Super Name: " + type.getSuperName());
    }
    //----------------------------------------------------------------------------------------------------------------------
    public void typeAttributesDemo(String typeName) throws DfException {
        IDfType type = session.getType(typeName);
        IDfAttr attr;
        for(int i = 0; i < type.getTypeAttrCount(); i++) {
            attr = type.getAttr(i);
            System.out.println("Name: " + attr.getName() + " Length: " + attr.getLength());
        }

    }
    //----------------------------------------------------------------------------------------------------------------------
    public void typeDumpDemo(String typeName) throws DfException {
        IDfType type = session.getType(typeName);

        System.out.println("Dump of " + typeName + ": ");
        System.out.println(type.dump());
    }
    //----------------------------------------------------------------------------------------------------------------------
    private void selectDocumentsForStudyDemo(String studyNumber) throws DfException {
        String dql = "select * from cd_clinical_tmf_doc where clinical_trial_id = '" + studyNumber + "'";
        IDfQuery query = new DfQuery();
        query.setDQL(dql);
        IDfCollection coll = null;
        coll = query.execute(session, IDfQuery.DF_READ_QUERY);

        while (coll.next())
        { System.out.println(coll.getString("r_object_id") + " " + coll.getString("object_name") + " " + coll.getString("acl_name"));  }
        if (coll != null) { coll.close(); }
    }
    //----------------------------------------------------------------------------------------------------------------------
    private void documentModificationDemo(String objectId) throws DfException {
        IDfSysObject sysObj = (IDfSysObject) session.getObject(new DfId(objectId));
        sysObj.setString("rog_comments", "test sample application");
        sysObj.save();
    }
    //----------------------------------------------------------------------------------------------------------------------
    public void apiDemo() throws DfException {
        IDfPersistentObject object = (IDfPersistentObject)session.newObject("dm_acl");
        object.apiSet("set", "owner_name", "etmfdev");
        object.apiSet("set", "object_name", "test_acl2");
        object.apiExec("grant", "dm_owner,1");
        object.apiExec("grant", "etmfdev,7,execute_proc,change_permit");
        object.save();
    }
    //----------------------------------------------------------------------------------------------------------------------
}
