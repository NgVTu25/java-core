package bt3.service.File;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileTracker {
	private static final Set<String> lockedFiles = ConcurrentHashMap.newKeySet();

	public static boolean tryLock(String filename) {
		return lockedFiles.add(filename);
	}

	public static void unlock(String filename) {
		lockedFiles.remove(filename);
	}

	public static boolean isLocked(String filename) {
		return lockedFiles.contains(filename);
	}
}