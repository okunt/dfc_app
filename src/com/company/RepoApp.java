package com.company;

import com.documentum.fc.client.*;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RepoApp {

    private Helper helper;
    private ArrayList<String> rObjectId = new ArrayList<>();

    //----------------------------------------------------------------------------------------------------------------------
    RepoApp() throws Exception {
        helper = new Helper();
        if(!helper.readConfig(helper.constants.CONFIG_FILE_NAME) || !helper.connect())  {
            System.out.println("Failed to read configuration file OR connect to Documentum repository");
            throw new Exception("");
        }
    }
    //----------------------------------------------------------------------------------------------------------------------
    public boolean invokeFunction() {
        try {
            switch (helper.prop.getProperty(helper.constants.APPLICATION_MODE)) {
                case "content_export":                                  return contentExport();
                case "properties_export":                               return propertiesExport(helper.prop.get(helper.constants.EXPORT_FILE_NAME).toString());
                case "permission_set_assignment":                       return permissionAssignment(helper.prop.getProperty(helper.constants.PERMISSION_SET_NAME));
                case "permission_sets_creation":                        return permissionsCreation(helper.prop.getProperty(helper.constants.PERMISSION_SETS_FILE_NAME));
                case "training_report":                                 return trainingReport();
                default:                                                return false;
            }
        } catch (DfException e) {
            e.printStackTrace();
            return false;
        }
    }
    //----------------------------------------------------------------------------------------------------------------------
    private boolean contentExport() {
        if(setObjectsIds(helper.prop.getProperty(helper.constants.OBJECT_LIST_FILE_PATH))) {
            while(!rObjectId.isEmpty()) {
                objectContentExport(rObjectId.remove(0),helper.prop.getProperty(helper.constants.EXPORT_FILE_NAME)); }
            return true;
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
        OutputStream out;
        try {
            sysObj = (IDfSysObject) helper.session.getObject(new DfId(objectId));
            contentType = sysObj.getContentType();
            objectName = sysObj.getObjectName().replace('/',' ');
            data = sysObj.getContent();
            out = new FileOutputStream(exportPath + objectName + "." + helper.getExtension(contentType));
        } catch (FileNotFoundException | DfException e) {
            e.printStackTrace();
            return false;
        }

        byte[] buf = new byte[1024];
        int len;
        while (true) {
            try {
                if (!((len = data.read(buf)) > 0)) break;
                out.write(buf, 0, len);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            data.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    //----------------------------------------------------------------------------------------------------------------------
    private boolean propertiesExport(String excelFileName) throws DfException {
        if(setObjectsIds(helper.prop.getProperty(helper.constants.OBJECT_LIST_FILE_PATH))) {
            if(helper.prop.getProperty(helper.constants.EXPORT_FILE_NAME).isEmpty())
                return false;
            else {
                if (!excelFileName.contains(".xlsx")) excelFileName += ".xlsx";
                Workbook wb = new XSSFWorkbook();
                Sheet sheet = wb.createSheet("Properties");
                IDfCollection coll;
                boolean firstQuery = true;
                Vector<String> propertyName = new Vector<>();
                int rowNumber = 0, cellNumber = 0, columnCount = 0;
                while(!rObjectId.isEmpty()) {
                    helper.query.setDQL("select * from dm_document where r_object_id = '" + rObjectId.remove(0) + "';");
                    coll = helper.query.execute(helper.session, IDfQuery.DF_READ_QUERY);
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
    private boolean permissionAssignment(String permissionSetName) throws DfException {
        if(setObjectsIds(helper.prop.getProperty(helper.constants.OBJECT_LIST_FILE_PATH))) {
            if(helper.prop.getProperty(helper.constants.PERMISSION_SET_NAME).isEmpty())
                return false;
            else {
                helper.query.setDQL("select owner_name from dm_acl where object_name = '"+ permissionSetName +"' enable(return_top 1);");
                IDfCollection coll = helper.query.execute(helper.session, IDfQuery.DF_READ_QUERY);
                String psDomain = "";
                if(coll.next()) {
                    psDomain = coll.getString("owner_name");
                }

                if (coll != null)   coll.close();

                while(!rObjectId.isEmpty()) {
                    helper.query.setDQL("update dm_document objects SET acl_domain = '"+ psDomain +"' SET acl_name = '" + permissionSetName + "' where r_object_id = '"+rObjectId.remove(0)+"';");
                    helper.query.execute(helper.session, IDfQuery.EXEC_QUERY);
                }
                return true;
            }
        }
        return false;
    }
    //----------------------------------------------------------------------------------------------------------------------
    private boolean permissionsCreation(String fileName) throws DfException {
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
                IDfPersistentObject object = (IDfPersistentObject)helper.session.newObject("dm_acl");
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
    private boolean setObjectsIds(String fileName) {
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
    private boolean trainingReport() {
        Map<String, String[]> map = new HashMap<>();
        map.put("user_name",    new String[]    {helper.prop.getProperty(helper.constants.USERNAME)});
        map.put("password",     new String[]    {helper.prop.getProperty(helper.constants.PASSWORD)});
        map.put("docbase_name", new String[]    {helper.prop.getProperty(helper.constants.REPOSITORY_NAME)});
        try {
            new TTMSTrainingReport(this.helper).execute(map, new PrintWriter(System.out));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
