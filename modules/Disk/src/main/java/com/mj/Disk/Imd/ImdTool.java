package com.mj.Disk.Imd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** A small host-side command-line tool for exploring real .IMD disk images -
 * the ImdImage/CpmImageReader counterpart to CpmDiskTool, which only ever reads
 * or writes this project's own in-house disk format. Read-only: this tool never
 * writes back to a real image, since the point is examining real period disks,
 * not modifying them.
 *
 * Run via, e.g.:
 *   mvn -pl modules/Disk exec:java -Dexec.mainClass=com.mj.Disk.Imd.ImdTool \
 *       -Dexec.args="list disk.imd trs80m2-cpm22" */
public class ImdTool {

	public static void main(String[] args) {
		try {
			run(args);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}

	private static void run(String[] args) throws Exception {
		if (args.length < 3) {
			printUsage();
			System.exit(1);
			return;
		}
		String command = args[0];
		Path imagePath = Paths.get(args[1]);
		DiskParameterBlock dpb = profile(args[2]);
		ImdImage image = ImdImage.read(imagePath);
		CpmImageReader reader = new CpmImageReader(image, dpb);

		if ("list".equals(command)) {
			list(reader);
		} else if ("get".equals(command)) {
			if (args.length < 5) {
				printUsage();
				System.exit(1);
				return;
			}
			get(reader, args[3], args[4]);
		} else {
			printUsage();
			System.exit(1);
		}
	}

	private static void list(CpmImageReader reader) {
		java.util.List<String> names = reader.listFiles();
		if (names.isEmpty()) {
			System.out.println("(no files)");
		} else {
			for (String name : names) {
				System.out.println(name);
			}
		}
	}

	private static void get(CpmImageReader reader, String cpmName, String hostFile) throws Exception {
		byte[] data = reader.readFile(cpmName);
		Files.write(Paths.get(hostFile), data);
		System.out.println("Read " + data.length + " bytes (rounded up to the nearest 128-byte record) from " + cpmName);
	}

	private static DiskParameterBlock profile(String name) {
		if ("trs80m2-cpm22".equals(name)) {
			return DiskParameterBlock.trs80ModelIiCpm22();
		}
		if ("compupro-cpmplus".equals(name)) {
			return DiskParameterBlock.compuProCpmPlus();
		}
		throw new IllegalArgumentException(
				"Unknown DPB profile: " + name + " (known: trs80m2-cpm22, compupro-cpmplus)");
	}

	private static void printUsage() {
		System.err.println("Usage:");
		System.err.println("  list <image.imd> <dpb-profile>");
		System.err.println("  get <image.imd> <dpb-profile> <cpm-name> <host-file>");
		System.err.println("Known DPB profiles: trs80m2-cpm22, compupro-cpmplus");
	}
}
