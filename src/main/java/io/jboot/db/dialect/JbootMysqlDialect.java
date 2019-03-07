/**
 * Copyright (c) 2015-2019, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jboot.db.dialect;

import com.jfinal.plugin.activerecord.dialect.MysqlDialect;
import io.jboot.db.model.Column;
import io.jboot.db.model.Or;
import io.jboot.utils.ArrayUtil;
import io.jboot.utils.StrUtil;

import java.util.List;


public class JbootMysqlDialect extends MysqlDialect implements IJbootModelDialect {

    @Override
    public String forFindByColumns(String table, String loadColumns, List<Column> columns, String orderBy, Object limit) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT ");
        sqlBuilder.append(loadColumns)
                .append(" FROM  `")
                .append(table).append("` ");

        appIfNotEmpty(columns, sqlBuilder);


        if (StrUtil.isNotBlank(orderBy)) {
            sqlBuilder.append(" ORDER BY ").append(orderBy);
        }

        if (limit != null) {
            sqlBuilder.append(" LIMIT " + limit);
        }

        return sqlBuilder.toString();
    }


    @Override
    public String forPaginateSelect(String loadColumns) {
        return "SELECT " + loadColumns;
    }


    @Override
    public String forPaginateFrom(String table, List<Column> columns, String orderBy) {
        StringBuilder sqlBuilder = new StringBuilder(" FROM `").append(table).append("`");

        appIfNotEmpty(columns, sqlBuilder);

        if (StrUtil.isNotBlank(orderBy)) {
            sqlBuilder.append(" ORDER BY ").append(orderBy);
        }

        return sqlBuilder.toString();
    }


    private void appIfNotEmpty(List<Column> columns, StringBuilder sqlBuilder) {
        if (ArrayUtil.isNotEmpty(columns)) {
            sqlBuilder.append(" WHERE ");

            int index = 0;
            int last = columns.size() - 1;
            for (Column column : columns) {
                if (column instanceof Or) {
                    // delete last " AND " str
                    sqlBuilder.delete(sqlBuilder.length() - 5,sqlBuilder.length())
                            .append(" OR ");
                }
                // in logic
                else  if (Column.LOGIC_IN.equals(column.getLogic())){
                    sqlBuilder.append(column.getName())
                            .append(" ")
                            .append(column.getLogic());

                    sqlBuilder.append("(");
                    Object[] values = (Object[]) column.getValue();
                    for (int i = 0 ;i<values.length;i++){
                        sqlBuilder.append("?,");
                    }
                    sqlBuilder.deleteCharAt(sqlBuilder.length() - 1).append(")");
                    if (index != last) {
                        sqlBuilder.append(" AND ");
                    }
                }

                // in between
                else if (Column.LOGIC_BETWEEN.equals(column.getLogic())){
                    sqlBuilder.append(column.getName())
                            .append(" ")
                            .append(column.getLogic());

                    sqlBuilder.append(" ? ").append(" AND ").append(" ? ");
                    if (index != last) {
                        sqlBuilder.append(" AND ");
                    }
                }
                // others
                else {
                    sqlBuilder.append(" `")
                            .append(column.getName())
                            .append("` ")
                            .append(column.getLogic());

                    if (column.isMustNeedValue()) {
                        sqlBuilder.append(" ? ");
                    }

                    if (index != last) {
                        sqlBuilder.append(" AND ");
                    }
                }
                index++;
            }
        }
    }

}
