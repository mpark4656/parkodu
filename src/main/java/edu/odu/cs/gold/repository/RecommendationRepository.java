package edu.odu.cs.gold.repository;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import edu.odu.cs.gold.model.Recommendation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class RecommendationRepository {
    private HazelcastInstance hazelcastInstance;
    private String collectionName;

    public RecommendationRepository(HazelcastInstance hazelcastInstance, String collectionName) {
        this.hazelcastInstance = hazelcastInstance;
        this.collectionName = collectionName;
    }

    public String getId(Recommendation entity) {
        return entity.getRecommendationKey();
    }

    public List<Recommendation> findAll() {
        IMap map = hazelcastInstance.getMap(collectionName);
        return new ArrayList<>(map.values());
    }

    public Recommendation findByKey(String key) {
        IMap map = hazelcastInstance.getMap(collectionName);
        return (Recommendation)map.get(key);
    }

    public List<Recommendation> findByKeys(Set<String> keys) {
        IMap map = hazelcastInstance.getMap(collectionName);
        return new ArrayList<>(map.getAll(keys).values());
    }

    public List<Recommendation> findByPredicate(Predicate predicate) {
        IMap map = hazelcastInstance.getMap(collectionName);
        return new ArrayList<>(map.values(predicate));
    }

    public void save(Recommendation entity) {
        IMap map = hazelcastInstance.getMap(collectionName);
        map.set(getId(entity), entity);
    }

    public void save(Collection<Recommendation> entities) {
        IMap map = hazelcastInstance.getMap(collectionName);
        for (Recommendation entity : entities) {
            map.set(getId(entity), entity);
        }
    }

    public void delete(String key) {
        IMap map = hazelcastInstance.getMap(collectionName);
        map.delete(key);
    }

    public int countByPredicate(Predicate predicate) {
        IMap map = hazelcastInstance.getMap(collectionName);
        return map.values(predicate).size();
    }

    public int deleteByPredicate(Predicate predicate) {
        IMap map = hazelcastInstance.getMap(collectionName);
        List<Recommendation> entities = this.findByPredicate(predicate);
        for (Recommendation entity : entities) {
            map.delete(getId(entity));
        }
        return entities.size();
    }

    public void loadAll() {
        IMap map = hazelcastInstance.getMap(collectionName);
        map.loadAll(false);
    }
}
