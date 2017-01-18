package de.kaidev.githubupdatelibrary;

/* Copyright (c) OSGi Alliance (4, 7). All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
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


/* Version identifier for bundles and packages.
*
* <p>
* Version identifiers have four components.
* <ol>
* <li>Major version. A non-negative integer.</li>
* <li>Minor version. A non-negative integer.</li>
* <li>Micro version. A non-negative integer.</li>
* <li>Qualifier. A text string. See <code>Version(String)</code> for the
* format of the qualifier string.</li>
* </ol>
*
* <p>
* <code>Version</code> objects are immutable.
*
* @since 1.3
* @Immutable
* @version $Revision: 1. $
*/

import android.support.annotation.NonNull;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Version implements Comparable<Version> {
    /**
     * The empty version "0.0.0". Equivalent to calling
     * <code>new Version(0,0,0)</code>.
     */
    public static final Version emptyVersion = new Version(0, 0, 0);

    private static final String SEPARATOR = ".";                    //$NON-NLS-1$
    private final int major;
    private final int minor;
    private final int micro;
    private final String qualifier;

    /**
     * Creates a version identifier from the specified numerical components.
     * <p>
     * <p>
     * The qualifier is set to the empty string.
     *
     * @param major Major component of the version identifier.
     * @param minor Minor component of the version identifier.
     * @param micro Micro component of the version identifier.
     * @throws IllegalArgumentException If the numerical components are
     *                                  negative.
     */
    public Version(int major, int minor, int micro) {
        this(major, minor, micro, null);
    }

    /**
     * Creates a version identifier from the specifed components.
     *
     * @param major     Major component of the version identifier.
     * @param minor     Minor component of the version identifier.
     * @param micro     Micro component of the version identifier.
     * @param qualifier Qualifier component of the version identifier. If
     *                  <code>null</code> is specified, then the qualifier will be set
     *                  to the empty string.
     * @throws IllegalArgumentException If the numerical components are negative
     *                                  or the qualifier string is invalid.
     */
    public Version(int major, int minor, int micro, String qualifier) {
        if (qualifier == null) {
            qualifier = ""; //$NON-NLS-1$
        }

        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = qualifier;
        validate();
    }

    /**
     * Created a version identifier from the specified string.
     * <p>
     * <p>
     * Here is the grammar for version strings.
     * <p>
     * <pre>
     * version ::= major('.'minor('.'micro('.'qualifier)?)?)?
     * major ::= digit+
     * minor ::= digit+
     * micro ::= digit+
     * qualifier ::= (alpha|digit|'_'|'-')+
     * digit ::= [0..9]
     * alpha ::= [a..zA..Z]
     * </pre>
     * <p>
     * There must be no whitespace in version.
     *
     * @param version String representation of the version identifier.
     * @throws IllegalArgumentException If <code>version</code> is improperly
     *                                  formatted.
     */
    public Version(String version) {
        int major = 0;
        int minor = 0;
        int micro = 0;
        String qualifier = ""; //$NON-NLS-1$

        try {
            StringTokenizer st = new StringTokenizer(version, SEPARATOR, true);
            major = Integer.parseInt(st.nextToken());

            if (st.hasMoreTokens()) {
                st.nextToken(); // consume delimiter
                minor = Integer.parseInt(st.nextToken());

                if (st.hasMoreTokens()) {
                    st.nextToken(); // consume delimiter
                    micro = Integer.parseInt(st.nextToken());

                    if (st.hasMoreTokens()) {
                        st.nextToken(); // consume delimiter
                        qualifier = st.nextToken();

                        if (st.hasMoreTokens()) {
                            throw new IllegalArgumentException("invalid format"); //$NON-NLS-1$
                        }
                    }
                }
            }
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("invalid format"); //$NON-NLS-1$
        }

        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = qualifier;
        validate();
    }

    /**
     * Parses a version identifier from the specified string.
     * <p>
     * <p>
     * See <code>Version(String)</code> for the format of the version string.
     *
     * @param version String representation of the version identifier. Leading
     *                and trailing whitespace will be ignored.
     * @return A <code>Version</code> object representing the version
     * identifier. If <code>version</code> is <code>null</code> or
     * the empty string then <code>emptyVersion</code> will be
     * returned.
     * @throws IllegalArgumentException If <code>version</code> is improperly
     *                                  formatted.
     */
    public static Version parseVersion(String version) {
        if (version == null) {
            return emptyVersion;
        }

        version = version.trim();
        if (version.length() == 0) {
            return emptyVersion;
        }

        return new Version(version);
    }

    /**
     * Called by the Version constructors to validate the version components.
     *
     * @throws IllegalArgumentException If the numerical components are negative
     *                                  or the qualifier string is invalid.
     */
    private void validate() {
        if (major < 0) {
            throw new IllegalArgumentException("negative major");
        }
        if (minor < 0) {
            throw new IllegalArgumentException("negative minor");
        }
        if (micro < 0) {
            throw new IllegalArgumentException("negative micro");
        }
        int length = qualifier.length();
        for (int i = 0; i < length; i++) {
            if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz9_-".indexOf(qualifier.charAt(i)) == -1) {
                throw new IllegalArgumentException("invalid qualifier");
            }
        }
    }

    /**
     * Returns the major component of this version identifier.
     *
     * @return The major component.
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor component of this version identifier.
     *
     * @return The minor component.
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Returns the micro component of this version identifier.
     *
     * @return The micro component.
     */
    public int getMicro() {
        return micro;
    }

    /**
     * Returns the qualifier component of this version identifier.
     *
     * @return The qualifier component.
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * Returns the string representation of this version identifier.
     * <p>
     * <p>
     * The format of the version string will be <code>major.minor.micro</code>
     * if qualifier is the empty string or
     * <code>major.minor.micro.qualifier</code> otherwise.
     *
     * @return The string representation of this version identifier.
     */
    public String toString() {
        String base = major + SEPARATOR + minor + SEPARATOR + micro;
        if (qualifier.length() == 0) {
            return base;
        } else {
            return base + SEPARATOR + qualifier;
        }
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return An integer which is a hash code value for this object.
     */
    public int hashCode() {
        return (major << 24) + (minor << 16) + (micro << 8)
                + qualifier.hashCode();
    }

    /**
     * Compares this <code>Version</code> object to another object.
     * <p>
     * <p>
     * A version is considered to be <b>equal to </b> another version if the
     * major, minor and micro components are equal and the qualifier component
     * is equal (using <code>String.equals</code>).
     *
     * @param object The <code>Version</code> object to be compared.
     * @return <code>true</code> if <code>object</code> is a
     * <code>Version</code> and is equal to this object;
     * <code>false</code> otherwise.
     */
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof Version)) {
            return false;
        }

        Version other = (Version) object;
        return (major == other.major) && (minor == other.minor)
                && (micro == other.micro) && qualifier.equals(other.qualifier);
    }

    /**
     * Compares this <code>Version</code> object to another object.
     * <p>
     * <p>
     * A version is considered to be <b>less than </b> another version if its
     * major component is less than the other version's major component, or the
     * major components are equal and its minor component is less than the other
     * version's minor component, or the major and minor components are equal
     * and its micro component is less than the other version's micro component,
     * or the major, minor and micro components are equal and it's qualifier
     * component is less than the other version's qualifier component (using
     * <code>String.compareTo</code>).
     * <p>
     * <p>
     * A version is considered to be <b>equal to</b> another version if the
     * major, minor and micro components are equal and the qualifier component
     * is equal (using <code>String.compareTo</code>).
     *
     * @param object The <code>Version</code> object to be compared.
     * @return A negative integer, zero, or a positive integer if this object is
     * less than, equal to, or greater than the specified
     * <code>Version</code> object.
     * @throws ClassCastException If the specified object is not a
     *                            <code>Version</code>.
     */
    public int compareTo(@NonNull Version object) {
        if (object == this) {
            return 0;
        }

        int result = major - object.major;
        if (result != 0) {
            return result;
        }

        result = minor - object.minor;
        if (result != 0) {
            return result;
        }

        result = micro - object.micro;
        if (result != 0) {
            return result;
        }

        return qualifier.compareTo(object.qualifier);
    }
}