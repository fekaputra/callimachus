/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
 *
 */
package org.callimachusproject.util;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainNameSystemResolver {
	private static final DomainNameSystemResolver instance = new DomainNameSystemResolver();

	public static DomainNameSystemResolver getInstance() {
		return instance;
	}

	private final Logger logger = LoggerFactory.getLogger(DomainNameSystemResolver.class);
	private final DirContext ictx;

	private DomainNameSystemResolver() {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put("java.naming.factory.initial",
				"com.sun.jndi.dns.DnsContextFactory");
		InitialDirContext initialDirContext;
		try {
			initialDirContext = new InitialDirContext(env);
		} catch (NumberFormatException e) {
			logger.warn(e.toString(), e);
			// can't parse IPv6
			initialDirContext = null;
		} catch (NamingException e) {
			logger.warn(e.toString(), e);
			initialDirContext = null;
		}
		ictx = initialDirContext;
	}

	public String lookup(String domain, String... type) throws NamingException {
		if (ictx == null)
			return null;
		Attributes attrs = ictx.getAttributes(domain, type);
		Enumeration<? extends Attribute> e = attrs.getAll();
		if (e.hasMoreElements()) {
			Attribute a = (Attribute) e.nextElement();
			int size = a.size();
			if (size > 0) {
				return (String) a.get(0);
			}
		}
		return null;
	}

	public InetAddress getLocalHost() {
		try {
			return InetAddress.getByName(null);
		} catch (UnknownHostException e) {
			try {
				final Enumeration<NetworkInterface> interfaces = NetworkInterface
						.getNetworkInterfaces();
				while (interfaces != null && interfaces.hasMoreElements()) {
					final Enumeration<InetAddress> addresses = interfaces
							.nextElement().getInetAddresses();
					while (addresses != null && addresses.hasMoreElements()) {
						InetAddress address = addresses.nextElement();
						if (address != null && address.isLoopbackAddress()) {
							return address;
						}
					}
				}
			} catch (SocketException se) {
			}
			throw new AssertionError("Unknown hostname: add the hostname of the machine to your /etc/hosts file.");
		}
	}

	public String getLocalHostName() {
		try {
			return InetAddress.getLocalHost().getHostName().toLowerCase();
		} catch (UnknownHostException e) {
			return "localhost";
		}
	}

	public String getCanonicalLocalHostName() {
		try {
			// attempt for the host canonical host name
			return InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
		} catch (UnknownHostException uhe) {
			try {
				// attempt to get the loop back address
				return InetAddress.getByName(null).getCanonicalHostName().toLowerCase();
			} catch (UnknownHostException uhe2) {
				// default to a standard loop back IP
				return "127.0.0.1";
			}
		}
	}

	public Collection<InetAddress> getAllLocalHosts() {
		Set<InetAddress> set = new TreeSet<InetAddress>();
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface
					.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				Enumeration<InetAddress> raddrs = iface.getInetAddresses();
				while (raddrs.hasMoreElements()) {
					set.add(raddrs.nextElement());
				}
				Enumeration<NetworkInterface> virtualIfaces = iface
						.getSubInterfaces();
				while (virtualIfaces.hasMoreElements()) {
					NetworkInterface viface = virtualIfaces.nextElement();
					Enumeration<InetAddress> vaddrs = viface.getInetAddresses();
					while (vaddrs.hasMoreElements()) {
						set.add(vaddrs.nextElement());
					}
				}
			}
		} catch (SocketException e) {
			// ignore
		}
		try {
			set.add(InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			// ignore
		}
		set.add(getLocalHost());
		set.remove(null);
		return set;
	}

	public boolean isNetworkLocalAddress(InetAddress iaddr) {
		if (iaddr.isLoopbackAddress())
			return true;
		try {
			byte[] addr = iaddr.getAddress();
			Enumeration<NetworkInterface> ifaces = NetworkInterface
					.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				for (InterfaceAddress ifconfig : iface.getInterfaceAddresses()) {
					if (isWithinSubnetMask(addr, ifconfig))
						return true;
				}
				Enumeration<NetworkInterface> virtualIfaces = iface
						.getSubInterfaces();
				while (virtualIfaces.hasMoreElements()) {
					NetworkInterface viface = virtualIfaces.nextElement();
					for (InterfaceAddress ifconfig : viface.getInterfaceAddresses()) {
						if (isWithinSubnetMask(addr, ifconfig))
							return true;
					}
				}
			}
		} catch (SocketException e) {
			// ignore
		}
		return false;
	}

	public Collection<String> reverseAllLocalHosts() throws SocketException {
		Set<String> set = new TreeSet<String>();
		for (InetAddress addr : getAllLocalHosts()) {
			addAllNames(addr, set);
		}
		return set;
	}

	public InetAddress getByName(String host) {
		try {
			return InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			return null;
		}
	}

	public String reverse(String ip) {
		try {
			return reverse(InetAddress.getByName(ip));
		} catch (UnknownHostException e) {
			return ip;
		}
	}

	public String reverse(InetAddress netAddr) {
		if (netAddr == null)
			return null;
		String name = netAddr.getCanonicalHostName().toLowerCase();
		try {
			if (!name.equals(netAddr.getHostAddress())
					&& netAddr.equals(InetAddress.getByName(name)))
				return name;
		} catch (UnknownHostException e) {
			// use reverse name
		}
		name = netAddr.getHostName().toLowerCase();
		try {
			if (!name.equals(netAddr.getHostAddress())
					&& netAddr.equals(InetAddress.getByName(name)))
				return name;
		} catch (UnknownHostException e) {
			// use reverse name
		}
		String address = getArpaName(netAddr);
		if (address == null)
			return name;
		try {
			String ptr = lookup(address, "PTR");
			if (ptr != null)
				return ptr;
		} catch (NamingException e) {
			// use reverse name
		}
		return address;
	}

	public String getArpaName(InetAddress netAddr) {
		byte[] addr = netAddr.getAddress();
		if (addr.length == 4) { // IPv4 Address
			StringBuilder sb = new StringBuilder();
			for (int i = addr.length - 1; i >= 0; i--) {
				sb.append((addr[i] & 0xff) + ".");
			}
			return sb.append("in-addr.arpa").toString();
		} else if (addr.length == 16) { // IPv6 Address
			StringBuilder sb = new StringBuilder();
			for (int i = addr.length - 1; i >= 0; i--) {
				sb.append(Integer.toHexString((addr[i] & 0x0f)));
				sb.append(".");
				sb.append(Integer.toHexString((addr[i] & 0xf0) >> 4));
				sb.append(".");
			}
			return sb.append("ip6.arpa").toString();
		}
		return null;
	}

	private void addAllNames(InetAddress addr, Set<String> set) {
		if (addr == null)
			return;
		set.add(addr.getHostAddress());
		set.add(addr.getHostName());
		set.add(addr.getCanonicalHostName());
		String address = getArpaName(addr);
		if (address != null) {
			set.add(address);
		}
	}

	private boolean isWithinSubnetMask(byte[] addr, InterfaceAddress ifconfig) {
		byte[] ifaddr = ifconfig.getAddress().getAddress();
		boolean mask = true;
		for (short i=0,n=ifconfig.getNetworkPrefixLength();i<n;i++) {
			mask &= ifaddr[i] == addr[i];
		}
		return mask;
	}
}
