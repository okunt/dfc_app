package com.company;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.*;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfLoginInfo;
import com.documentum.fc.methodserver.DfMethodArgumentManager;
import com.documentum.fc.methodserver.IDfMethod;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TTMSTrainingReport implements IDfMethod, IDfModule {

    private static final String GET_TRAINING_TEST_DQL = "select r_object_id, object_name, project_type, project_number, start_date from training_test";
    private static final String GET_MAX_REPORT_NUM_DQL = "select max(report_no) as rn from ttms_report";
    private static final String INSERT_TABLE_REPORT_DQL = "insert into ttms_report (report_no, report_date, r_object_id, object_name, project_type, project_number, start_date) values (";
    private static final String CHECK_ID_QUALIFICATION = "from ttms_report where r_object_id=";

    private static final int R_OBJECT_ID = 0;
    private static final int OBJECT_NAME = 1;
    private static final int PROJECT_TYPE = 2;
    private static final int PROJECT_NUMBER = 3;
    private static final int START_DATE = 4;

    private int reportNo;
    private Helper helper;


    TTMSTrainingReport(Helper helper) {
        this.helper = helper;

    }

    @Override
    public int execute(Map map, PrintWriter printWriter) throws Exception {

        setReportNum(helper.session);
        List<List<String>> data = getData(helper.session);
        populateRegisteredTable(helper.session, data);
        return 0;
    }

    /**
     * Set report number to global variable
     * @param session
     */
    private void setReportNum(IDfSession session) {
        runDQL(GET_MAX_REPORT_NUM_DQL, session, new IQueryHandler() {
            @Override
            public void process(IDfCollection row) throws DfException {
                reportNo = row.getInt("rn") + 1;
            }
        });
    }

    /**
     * Get documents data
     * @param session
     * @return
     */
    private List<List<String>> getData(IDfSession session) {
        final List<List<String>> data = new ArrayList();
        runDQL(GET_TRAINING_TEST_DQL, session, new IQueryHandler() {
            @Override
            public void process(IDfCollection row) throws DfException {
                List<String> dataRow = new ArrayList<>();
                dataRow.add(row.getString("r_object_id"));
                dataRow.add(row.getString("object_name"));
                dataRow.add(row.getString("project_type"));
                dataRow.add(row.getString("project_number"));
                dataRow.add(row.getString("start_date"));
                data.add(dataRow);
            }
        });

        return data;
    }

    /**
     * Create inserts for unique records and execute them
     * @param session
     * @param data
     */
    private void populateRegisteredTable(IDfSession session, List<List<String>> data) {
        for(List<String> row : data) {
            int count = runCountDQL(CHECK_ID_QUALIFICATION + Utils.escapeString(row.get(R_OBJECT_ID)), session);
            // insert only when there are no records with provided id in the registered table
            if(count == 0) {

                StringBuilder sb = new StringBuilder(INSERT_TABLE_REPORT_DQL);
                sb.append(reportNo).append(",");
                sb.append("DATE(NOW),");
                sb.append(Utils.escapeString(row.get(R_OBJECT_ID))).append(",");
                sb.append(Utils.escapeString(row.get(OBJECT_NAME))).append(",");
                sb.append(Utils.escapeString(row.get(PROJECT_TYPE))).append(",");
                sb.append(row.get(PROJECT_NUMBER)).append(",");
                String startDate = row.get(START_DATE);
                if(startDate.equals("nulldate")) {
                    sb.append("null");
                } else {
                    sb.append("DATE(").append(Utils.escapeString(startDate)).append(")");
                }
                sb.append(")");

                exec(sb.toString(), session);
            } else {
                System.out.println("Skipped. Count: " + count);
            }
        }
    }

    /**
     * Run DQL SELECT statements
     * @param dql
     * @param session
     * @param queryHandler
     */
    private void runDQL(String dql, IDfSession session, final IQueryHandler queryHandler) {

        helper.query.setDQL(dql);
        System.out.println("DQL: " + dql);

        IDfCollection col = null;
        try {
            col = helper.query.execute(session, IDfQuery.READ_QUERY);
            while (col.next()) {
                queryHandler.process(col);
            }

        } catch (DfException e) {
            System.err.println(e.getMessage() + " b l a " );
        } finally {
            if (col != null) {
                try {
                    col.close();
                } catch (DfException e) {
                    System.out.println("Error");
                }
            }
        }
    }

    /**
     * Run count query
     * @param qualification
     * @param session
     * @return
     */
    private int runCountDQL(String qualification, IDfSession session) {

        helper.query.setDQL("select count(*) as c " + qualification);
        System.out.println("COUNT: " + qualification);

        IDfCollection col = null;
        try {
            col = helper.query.execute(session, IDfQuery.READ_QUERY);
            while (col.next()) {
                return col.getInt("c");
            }

        } catch (DfException e) {
            System.err.println(e.getMessage());
        } finally {
            if (col != null) {
                try {
                    col.close();
                } catch (DfException e) {
                    System.out.println("Error");
                }
            }
        }

        return -1;
    }

    /**
     * Execute any DQL statement
     * @param dql
     * @param session
     */
    private void exec(String dql, IDfSession session) {

        helper.query.setDQL(dql);
        System.out.println("STATEMENT: " + dql);

        IDfCollection col = null;
        try {
            col = helper.query.execute(session, IDfQuery.EXEC_QUERY);
        } catch (DfException e) {
            System.err.println(e.getMessage());
        } finally {
            if (col != null) {
                try {
                    col.close();
                } catch (DfException e) {
                    System.out.println("Error");
                }
            }
        }
    }
}
