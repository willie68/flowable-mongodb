/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.mongodb.persistence.manager;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.data.VariableInstanceDataManager;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceByExecutionIdMatcher;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;

/**
 * @author Joram Barrez
 */
public class MongoDbVariableInstanceDataManager extends AbstractMongoDbDataManager<VariableInstanceEntity> implements VariableInstanceDataManager {

    public static final String COLLECTION_VARIABLES = "variables";

    protected VariableInstanceByExecutionIdMatcher variableInstanceByExecutionIdMatcher = new VariableInstanceByExecutionIdMatcher();

    @Override
    public String getCollection() {
        return COLLECTION_VARIABLES;
    }

    @Override
    public VariableInstanceEntity create() {
        return new VariableInstanceEntityImpl();
    }

    @Override
    public BasicDBObject createUpdateObject(Entity entity) {
        VariableInstanceEntity variableEntity = (VariableInstanceEntity) entity;
        BasicDBObject updateObject = null;
        updateObject = setUpdateProperty(variableEntity, "textValue", variableEntity.getTextValue(), updateObject);
        updateObject = setUpdateProperty(variableEntity, "textValue2", variableEntity.getTextValue2(), updateObject);
        updateObject = setUpdateProperty(variableEntity, "doubleValue", variableEntity.getDoubleValue(), updateObject);
        updateObject = setUpdateProperty(variableEntity, "longValue", variableEntity.getLongValue(), updateObject);
        updateObject = setUpdateProperty(variableEntity, "typeName", variableEntity.getTypeName(), updateObject);
        return updateObject;
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskId(String taskId) {
        return getMongoDbSession().find(COLLECTION_VARIABLES, Filters.eq("taskId", taskId));
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskIds(Set<String> taskIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionId(String executionId) {
        return getMongoDbSession().find(COLLECTION_VARIABLES, Filters.eq("executionId", executionId), executionId,
                VariableInstanceEntityImpl.class, variableInstanceByExecutionIdMatcher, true);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionIds(Set<String> executionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByExecutionAndName(String executionId, String variableName) {
        Bson filter = Filters.and(Filters.eq("executionId", executionId), Filters.eq("name", variableName));
        return getMongoDbSession().findOne(COLLECTION_VARIABLES, filter);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionAndNames(String executionId, Collection<String> names) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByTaskAndName(String taskId, String variableName) {
        Bson filter = Filters.and(Filters.eq("taskId", taskId), Filters.eq("name", variableName));
        return getMongoDbSession().findOne(COLLECTION_VARIABLES, filter);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskAndNames(String taskId, Collection<String> names) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstanceByScopeIdAndScopeType(String scopeId, String scopeType) {
        Bson filter = Filters.and(Filters.eq("scopeId", scopeId), Filters.eq("scopeType", scopeType));
        return getMongoDbSession().findOne(COLLECTION_VARIABLES, filter);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByScopeIdAndScopeTypeAndName(String scopeId, String scopeType, String variableName) {
        Bson filter = Filters.and(Filters.eq("scopeId", scopeId), Filters.eq("scopeType", scopeType),
                Filters.eq("name", variableName));
        return getMongoDbSession().findOne(COLLECTION_VARIABLES, filter);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByScopeIdAndScopeTypeAndNames(String scopeId,
            String scopeType, Collection<String> variableNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstanceBySubScopeIdAndScopeType(String subScopeId,
            String scopeType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableInstanceEntity findVariableInstanceBySubScopeIdAndScopeTypeAndName(String subScopeId,
            String scopeType, String variableName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesBySubScopeIdAndScopeTypeAndNames(String subScopeId,
            String scopeType, Collection<String> variableNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteVariablesByTaskId(String taskId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteVariablesByExecutionId(String executionId) {
        List<VariableInstanceEntity> variables = findVariableInstancesByExecutionId(executionId);
        if (variables != null) {
            for (VariableInstanceEntity variableInstanceEntity : variables) {
                getMongoDbSession().delete(COLLECTION_VARIABLES, variableInstanceEntity);
            }
        }
    }

    @Override
    public void deleteByScopeIdAndScopeType(String scopeId, String scopeType) {
        throw new UnsupportedOperationException();
    }

    public VariableInstanceEntityImpl transformToEntity(Document document) {
        throw new UnsupportedOperationException();
    }

}
