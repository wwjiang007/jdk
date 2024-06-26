/*
 * Copyright (c) 1999, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * <p>Provides the definition of the monitor classes.  A Monitor is
 * an MBean that periodically observes the value of an attribute in
 * one or more other MBeans.  If the attribute meets a certain
 * condition, the Monitor emits a {@link
 * javax.management.monitor.MonitorNotification
 * MonitorNotification}. When the monitor MBean periodically calls
 * {@link javax.management.MBeanServer#getAttribute getAttribute}
 * to retrieve the value of the attribute being monitored it does
 * so within the access control context of the
 * {@link javax.management.monitor.Monitor#start} caller.</p>
 *
 * <p id="complex">The value being monitored can be a simple value
 * contained within a complex type. For example, the {@link
 * java.lang.management.MemoryMXBean MemoryMXBean} defined in
 * {@code java.lang.management} has an attribute
 * {@code HeapMemoryUsage} of type {@link
 * java.lang.management.MemoryUsage MemoryUsage}. To monitor the
 * amount of <i>used</i> memory, described by the {@code used}
 * property of {@code MemoryUsage}, you could monitor
 * "{@code HeapMemoryUsage.used}". That string would be the
 * argument to {@link
 * javax.management.monitor.MonitorMBean#setObservedAttribute(String)
 * setObservedAttribute}.</p>
 *
 * <p>The rules used to interpret an {@code ObservedAttribute} like
 * {@code "HeapMemoryUsage.used"} are as follows. Suppose the string is
 * <i>A.e</i> (so <i>A</i> would be {@code "HeapMemoryUsage"} and <i>e</i>
 * would be {@code "used"} in the example).</p>
 *
 * <p>First the value of the attribute <i>A</i> is obtained. Call it
 * <i>v</i>. A value <i>x</i> is extracted from <i>v</i> as follows:</p>
 *
 *   <ul>
 *
 *   <li>If <i>v</i> is a {@link javax.management.openmbean.CompositeData
 *   CompositeData} and if <i>v</i>.{@link
 *   javax.management.openmbean.CompositeData#get(String) get}(<i>e</i>)
 *   returns a value then <i>x</i> is that value.</li>
 *   <li>If <i>v</i> is an array and <i>e</i> is the string {@code "length"}
 *   then <i>x</i> is the length of the array.</li>
 *
 *   <li>If the above rules do not produce a value, and if introspection, as
 *   if by calling <a href="{@docRoot}/java.desktop/java/beans/Introspector.html#getBeanInfo(java.lang.Class)">Introspector.getBeanInfo</a>
 *   , for the class of <i>v</i>
 *   (<i>v</i>.{@code getClass()}) identifies a property with the name
 *   <i>e</i>, then <i>x</i> is the result of reading the property value. </li>
 *
 *   </ul>
 *
 *   <p>The third rule means for example that if the attribute
 *   {@code HeapMemoryUsage} is a {@code MemoryUsage}, monitoring
 *   {@code "HeapMemoryUsage.used"} will obtain the observed value by
 *   calling {@code MemoryUsage.getUsed()}.</p>
 *
 *   <p>If the {@code ObservedAttribute} contains more than one period,
 *   for example {@code "ConnectionPool.connectionStats.length"}, then the
 *   above rules are applied iteratively. Here, <i>v</i> would initially be
 *   the value of the attribute {@code ConnectionPool}, and <i>x</i> would
 *   be derived by applying the above rules with <i>e</i> equal to
 *   {@code "connectionStats"}. Then <i>v</i> would be set to this <i>x</i>
 *   and a new <i>x</i> derived by applying the rules again with <i>e</i>
 *   equal to {@code "length"}.</p>
 *
 *   <p>Although it is recommended that attribute names be valid Java
 *   identifiers, it is possible for an attribute to be called
 *   {@code HeapMemoryUsage.used}. This means that an
 *   {@code ObservedAttribute} that is {@code HeapMemoryUsage.used}
 *   could mean that the value to observe is either an attribute of that
 *   name, or the property {@code used} within an attribute called
 *   {@code HeapMemoryUsage}. So for compatibility reasons, when the
 *   {@code ObservedAttribute} contains a period ({@code .}), the monitor
 *   will check whether an attribute exists whose name is the full
 *   {@code ObservedAttribute} string ({@code HeapMemoryUsage.used} in the
 *   example). It does this by calling {@link
 *   javax.management.MBeanServer#getMBeanInfo(javax.management.ObjectName)
 *   getMBeanInfo} for the observed MBean and looking for a contained {@link
 *   javax.management.MBeanAttributeInfo MBeanAttributeInfo} with the given
 *   name. If one is found, then that is what is monitored. If more than one
 *   MBean is being observed, the behavior is unspecified if some of them have
 *   a {@code HeapMemoryUsage.used} attribute and others do not. An
 *   implementation may therefore call {@code getMBeanInfo} on just one of
 *   the MBeans in this case. The behavior is also unspecified if the result
 *   of the check changes while the monitor is active.</p>
 *
 *   <p>The exact behavior of monitors is detailed in the
 * <a href="#spec">JMX Specification</a>.  What follows is a
 * summary.</p>
 *
 *   <p>There are three kinds of Monitors:</p>
 *
 *   <ul>
 * <li>
 *
 *   <p>A {@link javax.management.monitor.CounterMonitor
 *     CounterMonitor} observes attributes of integer
 *     type.  The attributes are assumed to be non-negative, and
 *     monotonically increasing except for a possible
 *     <em>roll-over</em> at a specified <em>modulus</em>.  Each
 *     observed attribute has an associated <em>threshold</em>
 *     value.  A notification is sent when the attribute exceeds
 *     its threshold.</p>
 *
 *   <p>An <em>offset</em> value can be specified.  When an
 *     observed value exceeds its threshold, the threshold is
 *     incremented by the offset, or by a multiple of the offset
 *     sufficient to make the threshold greater than the new
 *     observed value.</p>
 *
 *   <p>A <code>CounterMonitor</code> can operate in
 *     <em>difference mode</em>.  In this mode, the value
 *     compared against the threshold is the difference between
 *     two successive observations of an attribute.</p>
 *
 * </li>
 * <li>
 *
 *   <p>A {@link javax.management.monitor.GaugeMonitor
 *     GaugeMonitor} observes attributes of numerical type.  Each
 *     observed attribute has an associated <em>high
 *       threshold</em> and <em>low threshold</em>.</p>
 *
 *   <p>When an observed attribute crosses the high threshold, if
 *     the <em>notify high</em> flag is true, then a notification
 *     is sent.  Subsequent crossings of the high threshold value
 *     will not trigger further notifications until the gauge value
 *     becomes less than or equal to the low threshold.</p>
 *
 *   <p>When an observed attribute crosses the low threshold, if
 *     the <em>notify low</em> flag is true, then a notification
 *     is sent.  Subsequent crossings of the low threshold value
 *     will not trigger further notifications until the gauge
 *     value becomes greater than or equal to the high
 *     threshold.</p>
 *
 *   <p>Typically, only one of the notify high and notify low
 *     flags is set.  The other threshold is used to provide a
 *     <em>hysteresis</em> mechanism to avoid the repeated
 *     triggering of notifications when an attribute makes small
 *     oscillations around the threshold value.</p>
 *
 *   <p>A <code>GaugeMonitor</code> can operate in <em>difference
 *       mode</em>.  In this mode, the value compared against the
 *     high and low thresholds is the difference between two
 *     successive observations of an attribute.</p>
 *
 * </li>
 * <li>
 *
 *   <p>A {@link javax.management.monitor.StringMonitor
 *     StringMonitor} observes attributes of type
 *     <code>String</code>.  A notification is sent when an
 *     observed attribute becomes equal and/or not equal to a
 *     given string.</p>
 *
 * </li>
 *   </ul>
 * @see <a id="spec" href="https://jcp.org/aboutJava/communityprocess/mrel/jsr160/index2.html">
 *   JMX Specification, version 1.4</a>
 *   @since 1.5
 */
package javax.management.monitor;
