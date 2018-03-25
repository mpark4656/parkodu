package edu.odu.cs.gold.model;

import java.io.Serializable;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import org.springframework.security.core.GrantedAuthority;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.*;


/*
 *
 * Current RoleType Types:
 * - user
 * - admin
 *
 */

public class RoleType {

    private String roleKey;
    private String name;
    private Integer accessLevel;
    private String accessLevelName;

    public RoleType() { }

    public RoleType(String name) {
        this.name = name;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAccessLevel(Integer accessLevel) {
        this.accessLevel = accessLevel;
    }

    public Integer getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevelName(String accessLevelName) {
        this.accessLevelName = accessLevelName;
    }

    public String getAccessLevelName() {
        return accessLevelName;
    }

    @Override
    public String toString() {
        return "RoleType{" +
                "roleKey='" + roleKey + '\'' +
                ", name='" + name + '\'' +
                ", accessLevel='" + accessLevel + '\'' +
                ", accessLevelName='" + accessLevelName + '\'' +
                '}';
    }

}
