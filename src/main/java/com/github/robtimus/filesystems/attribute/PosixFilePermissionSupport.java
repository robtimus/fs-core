/*
 * PosixFilePermissionSupport.java
 * Copyright 2016 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.filesystems.attribute;

import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * A utility class for {@link PosixFilePermission}.
 *
 * @author Rob Spoor
 * @since 1.1
 */
public final class PosixFilePermissionSupport {

    private static final int S_IRUSR = 0400;
    private static final int S_IWUSR = 0200;
    private static final int S_IXUSR = 0100;
    private static final int S_IRGRP = 040;
    private static final int S_IWGRP = 020;
    private static final int S_IXGRP = 010;
    private static final int S_IROTH = 04;
    private static final int S_IWOTH = 02;
    private static final int S_IXOTH = 01;

    private PosixFilePermissionSupport() {
        throw new IllegalStateException("cannot create instances of " + getClass().getName()); //$NON-NLS-1$
    }

    /**
     * Returns the set of permissions corresponding to a permission bit mask. This method uses the most usual mapping:
     * <ul>
     * <li>0400 maps to {@link PosixFilePermission#OWNER_READ}</li>
     * <li>0200 maps to {@link PosixFilePermission#OWNER_WRITE}</li>
     * <li>0100 maps to {@link PosixFilePermission#OWNER_EXECUTE}</li>
     * <li>0040 maps to {@link PosixFilePermission#GROUP_READ}</li>
     * <li>0020 maps to {@link PosixFilePermission#GROUP_WRITE}</li>
     * <li>0010 maps to {@link PosixFilePermission#GROUP_EXECUTE}</li>
     * <li>0004 maps to {@link PosixFilePermission#OTHERS_READ}</li>
     * <li>0002 maps to {@link PosixFilePermission#OTHERS_WRITE}</li>
     * <li>0001 maps to {@link PosixFilePermission#OTHERS_EXECUTE}</li>
     * </ul>
     *
     * @param mask The bit mask representing a set of permissions.
     * @return The resulting set of permissions.
     */
    public static Set<PosixFilePermission> fromMask(int mask) {
        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
        addIfSet(permissions, mask, S_IRUSR, PosixFilePermission.OWNER_READ);
        addIfSet(permissions, mask, S_IWUSR, PosixFilePermission.OWNER_WRITE);
        addIfSet(permissions, mask, S_IXUSR, PosixFilePermission.OWNER_EXECUTE);
        addIfSet(permissions, mask, S_IRGRP, PosixFilePermission.GROUP_READ);
        addIfSet(permissions, mask, S_IWGRP, PosixFilePermission.GROUP_WRITE);
        addIfSet(permissions, mask, S_IXGRP, PosixFilePermission.GROUP_EXECUTE);
        addIfSet(permissions, mask, S_IROTH, PosixFilePermission.OTHERS_READ);
        addIfSet(permissions, mask, S_IWOTH, PosixFilePermission.OTHERS_WRITE);
        addIfSet(permissions, mask, S_IXOTH, PosixFilePermission.OTHERS_EXECUTE);
        return permissions;
    }

    private static void addIfSet(Set<PosixFilePermission> permissions, int mask, int bits, PosixFilePermission permission) {
        if (isSet(mask, bits)) {
            permissions.add(permission);
        }
    }

    /**
     * Returns a permission bit mask corresponding to a set of permissions. This method is the inverse of {@link #fromMask(int)}.
     *
     * @param permissions The set of permissions.
     * @return The resulting permission bit mask.
     */
    public static int toMask(Set<PosixFilePermission> permissions) {
        int mask = 0;
        mask |= getBitsIfSet(permissions, S_IRUSR, PosixFilePermission.OWNER_READ);
        mask |= getBitsIfSet(permissions, S_IWUSR, PosixFilePermission.OWNER_WRITE);
        mask |= getBitsIfSet(permissions, S_IXUSR, PosixFilePermission.OWNER_EXECUTE);
        mask |= getBitsIfSet(permissions, S_IRGRP, PosixFilePermission.GROUP_READ);
        mask |= getBitsIfSet(permissions, S_IWGRP, PosixFilePermission.GROUP_WRITE);
        mask |= getBitsIfSet(permissions, S_IXGRP, PosixFilePermission.GROUP_EXECUTE);
        mask |= getBitsIfSet(permissions, S_IROTH, PosixFilePermission.OTHERS_READ);
        mask |= getBitsIfSet(permissions, S_IWOTH, PosixFilePermission.OTHERS_WRITE);
        mask |= getBitsIfSet(permissions, S_IXOTH, PosixFilePermission.OTHERS_EXECUTE);
        return mask;
    }

    private static int getBitsIfSet(Set<PosixFilePermission> permissions, int bits, PosixFilePermission permission) {
        return permissions.contains(permission) ? bits : 0;
    }

    /**
     * Returns whether or not a specific permission is set in a permission bit mask.
     * <p>
     * More formally, this method returns {@code true} only if the given permission is contained in the set returned by {@link #fromMask(int)}.
     *
     * @param mask The permission bit mask to check.
     * @param permission The permission to check for.
     * @return {@code true} if the permission is set in the given permission bit mask, or {@code false} otherwise.
     */
    public static boolean hasPermission(int mask, PosixFilePermission permission) {
        switch (permission) {
            case OWNER_READ:
                return isSet(mask, S_IRUSR);
            case OWNER_WRITE:
                return isSet(mask, S_IWUSR);
            case OWNER_EXECUTE:
                return isSet(mask, S_IXUSR);
            case GROUP_READ:
                return isSet(mask, S_IRGRP);
            case GROUP_WRITE:
                return isSet(mask, S_IWGRP);
            case GROUP_EXECUTE:
                return isSet(mask, S_IXGRP);
            case OTHERS_READ:
                return isSet(mask, S_IROTH);
            case OTHERS_WRITE:
                return isSet(mask, S_IWOTH);
            case OTHERS_EXECUTE:
                return isSet(mask, S_IXOTH);
            default:
                // should not occur
                return false;
        }
    }

    private static boolean isSet(int mask, int bits) {
        return (mask & bits) != 0;
    }
}
