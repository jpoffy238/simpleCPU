package com.mj.Disk;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.mj.Disk.EightInch.IBM3740;

/** A small host-side command-line tool for preparing CP/M-formatted disk images
 * before any 6502-resident DOS exists to do it from inside the emulator - format
 * a blank image, copy files in and out, list what's there. Only IBM3740 geometry
 * is supported, since that's the only drive type this project has so far.
 *
 * Run via, e.g.:
 *   mvn -pl modules/Disk exec:java -Dexec.mainClass=com.mj.Disk.CpmDiskTool \
 *       -Dexec.args="format disk0.img" */
public class CpmDiskTool {

	public static void main(String[] args) {
		try {
			run(args);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}

	private static void run(String[] args) throws Exception {
		if (args.length < 2) {
			printUsage();
			System.exit(1);
			return;
		}
		String command = args[0];
		String imagePath = args[1];

		if ("format".equals(command)) {
			format(imagePath);
		} else if ("put".equals(command)) {
			requireArgs(args, 4, "put <image> <host-file> <cpm-name>");
			put(imagePath, args[2], args[3]);
		} else if ("get".equals(command)) {
			requireArgs(args, 4, "get <image> <cpm-name> <host-file>");
			get(imagePath, args[2], args[3]);
		} else if ("list".equals(command)) {
			list(imagePath);
		} else {
			printUsage();
			System.exit(1);
		}
	}

	private static void requireArgs(String[] args, int count, String usage) {
		if (args.length < count) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}
	}

	private static IBM3740 openExisting(String imagePath) throws Exception {
		IBM3740 drive = new IBM3740();
		drive.setVirtualDiskFile(imagePath);
		return drive;
	}

	private static void format(String imagePath) throws Exception {
		IBM3740 drive = new IBM3740();
		drive.setVirtualDiskFile(imagePath);
		drive.format(); // force a fresh, fully blank image, even if one already existed
		new CpmFileSystem(drive).format();
		System.out.println("Formatted " + imagePath);
	}

	private static void put(String imagePath, String hostFile, String cpmName) throws Exception {
		IBM3740 drive = openExisting(imagePath);
		byte[] data = Files.readAllBytes(Paths.get(hostFile));
		new CpmFileSystem(drive).createFile(cpmName, data);
		System.out.println("Wrote " + data.length + " bytes to " + cpmName);
	}

	private static void get(String imagePath, String cpmName, String hostFile) throws Exception {
		IBM3740 drive = openExisting(imagePath);
		byte[] data = new CpmFileSystem(drive).readFile(cpmName);
		Files.write(Paths.get(hostFile), data);
		System.out.println("Read " + data.length + " bytes (rounded up to the nearest 128-byte record) from " + cpmName);
	}

	private static void list(String imagePath) throws Exception {
		IBM3740 drive = openExisting(imagePath);
		List<String> names = new CpmFileSystem(drive).listFiles();
		if (names.isEmpty()) {
			System.out.println("(no files)");
		} else {
			for (String name : names) {
				System.out.println(name);
			}
		}
	}

	private static void printUsage() {
		System.err.println("Usage:");
		System.err.println("  format <image>");
		System.err.println("  put <image> <host-file> <cpm-name>");
		System.err.println("  get <image> <cpm-name> <host-file>");
		System.err.println("  list <image>");
	}
}
