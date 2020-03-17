package amidst.gui.seedsearcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.function.Consumer;

import amidst.documentation.AmidstThread;
import amidst.documentation.CalledOnlyBy;
import amidst.documentation.NotThreadSafe;
import amidst.gui.main.MainWindowDialogs;
import amidst.logging.AmidstLogger;
import amidst.mojangapi.LauncherProfileRunner;
import amidst.mojangapi.RunningLauncherProfile;
import amidst.mojangapi.file.LauncherProfile;
import amidst.mojangapi.file.MinecraftInstallation;
import amidst.mojangapi.file.PlayerInformationCache;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaceCreationException;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaceException;
import amidst.mojangapi.world.SeedHistoryLogger;
import amidst.mojangapi.world.World;
import amidst.mojangapi.world.WorldBuilder;
import amidst.mojangapi.world.WorldOptions;
import amidst.mojangapi.world.WorldSeed;
import amidst.mojangapi.world.WorldType;
import amidst.mojangapi.world.filter.WorldFilter;
import amidst.threading.ThreadMaster;
import amidst.threading.WorkerExecutor;
import amidst.threading.worker.ProgressReporter;
import amidst.threading.worker.ProgressReportingWorker;

@NotThreadSafe
public class SeedSearcher {
	private final MainWindowDialogs dialogs;
	private final RunningLauncherProfile runningLauncherProfile;
	private final WorkerExecutor workerExecutor;

	private volatile boolean isSearching = false;
	private volatile boolean isStopRequested = false;
	
	public static void main(String args[]) throws Exception {
		start(getInitialLauncherProfile());
	}
	
	@SuppressWarnings("resource")
	private static void start(Optional<LauncherProfile> preferredLauncherProfile) throws MinecraftInterfaceCreationException, IOException {
		boolean continuusSearch = true;
		System.out.println("test");
		SeedHistoryLogger seedHistoryLogger = SeedHistoryLogger.from(new File("/tmp/SeedHist.txt"));
		WorldBuilder worldBuilder = new WorldBuilder(seedHistoryLogger);
		LauncherProfileRunner launcherProfileRunner = new LauncherProfileRunner(worldBuilder, Optional.empty());
		//Optional<LauncherProfile> preferredLauncherProfile;
		RunningLauncherProfile runningLauncherProfile = launcherProfileRunner.run(preferredLauncherProfile.get());
		WorldFilter mf = new MySeedFilter(1024);
		SeedSearcherConfiguration sc = new SeedSearcherConfiguration(mf,WorldType.from("Default"),continuusSearch);
		
		FileWriter fileWriter = new FileWriter("seeds.txt");
	    PrintWriter printWriter = new PrintWriter(fileWriter);
	    File fileg = new File("seeds.txt");
		FileWriter frg;
	    frg = new FileWriter(fileg, true);
		frg.write("New run:\n");
		frg.close();
		Consumer<WorldOptions> display = a -> {
			File file = new File("seeds.txt");
			FileWriter fr;
			try {
				fr = new FileWriter(file, true);
				fr.write(""+a.getWorldSeed().getLong()+"\n");
				fr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Found seed: "+a.getWorldSeed().getLong());
			};
		printWriter.close();
		ThreadMaster tm = new ThreadMaster();
		SeedSearcher seedSearcher = new SeedSearcher(
				null,
				runningLauncherProfile.createSilentPlayerlessCopy(),
				tm.getWorkerExecutor());
		seedSearcher.search(sc,display);
	}
	
	public static Optional<LauncherProfile> getInitialLauncherProfile() throws Exception {
		MinecraftInstallation minecraftInstallation = MinecraftInstallation.newLocalMinecraftInstallation(new File("C:\\Users\\Lukas\\AppData\\Roaming\\.minecraft"));
		//MinecraftInstallation minecraftInstallation = MinecraftInstallation.newLocalMinecraftInstallation(new File("/home/lukas/.minecraft"));
		/*for (LauncherProfile temp : minecraftInstallation.readInstalledVersionsAsLauncherProfiles()) {
		    System.out.println(temp.getProfileName());
		}*/
		//System.out.println(minecraftInstallation.readInstalledVersionsAsLauncherProfiles().get(3).getProfileName());
		for(LauncherProfile prof : minecraftInstallation.readInstalledVersionsAsLauncherProfiles()) {
			System.out.println(prof.getProfileName());
			if(prof.getProfileName().equals("1.15.2")) {
				System.out.println(prof.getProfileName());
				return Optional.of(prof);
			}
		}
		System.out.println("NO 1.15.2 PROFILE FOUND!!!");
		return Optional.of(minecraftInstallation.readInstalledVersionsAsLauncherProfiles().get(3));
		//System.out.println(minecraftInstallation.readInstalledVersionsAsLauncherProfiles().getProfileName());
		/*String profileName = "1.15.2";
	    if (profileName != null) {
	    	System.out.println(minecraftInstallation.tryGetLauncherProfileFromName(profileName));
	        return minecraftInstallation.tryGetLauncherProfileFromName(profileName);
	    }
	    return Optional.empty();*/
	}

	@CalledOnlyBy(AmidstThread.EDT)
	public SeedSearcher(
			MainWindowDialogs dialogs,
			RunningLauncherProfile runningLauncherProfile,
			WorkerExecutor workerExecutor) {
		this.dialogs = dialogs;
		this.runningLauncherProfile = runningLauncherProfile;
		this.workerExecutor = workerExecutor;
	}

	@CalledOnlyBy(AmidstThread.EDT)
	public void search(SeedSearcherConfiguration configuration, Consumer<WorldOptions> onWorldFound) {
		this.isSearching = true;
		this.isStopRequested = false;
		workerExecutor.run(createSearcher(configuration), onWorldFound);
	}

	@CalledOnlyBy(AmidstThread.EDT)
	private ProgressReportingWorker<WorldOptions> createSearcher(SeedSearcherConfiguration configuration) {
		return reporter -> this.trySearch(reporter, configuration);
	}

	@CalledOnlyBy(AmidstThread.EDT)
	public void stop() {
		this.isStopRequested = true;
	}

	@CalledOnlyBy(AmidstThread.EDT)
	public void dispose() {
		stop();
	}

	@CalledOnlyBy(AmidstThread.EDT)
	public boolean isSearching() {
		return isSearching;
	}

	@CalledOnlyBy(AmidstThread.EDT)
	public boolean isStopRequested() {
		return isStopRequested;
	}

	@CalledOnlyBy(AmidstThread.WORKER)
	private void trySearch(ProgressReporter<WorldOptions> reporter, SeedSearcherConfiguration configuration) {
		try {
			doSearch(reporter, configuration);
		} catch (IllegalStateException | MinecraftInterfaceException e) {
			AmidstLogger.warn(e);
			dialogs.displayError(e);
		} finally {
			this.isSearching = false;
			this.isStopRequested = false;
		}
	}

	@CalledOnlyBy(AmidstThread.WORKER)
	private void doSearch(ProgressReporter<WorldOptions> reporter, SeedSearcherConfiguration configuration)
			throws IllegalStateException,
			MinecraftInterfaceException {
		do {
			doSearchOne(reporter, configuration);
		} while (configuration.isSearchContinuously() && !isStopRequested);
	}

	@CalledOnlyBy(AmidstThread.WORKER)
	private void doSearchOne(ProgressReporter<WorldOptions> reporter, SeedSearcherConfiguration configuration)
			throws IllegalStateException,
			MinecraftInterfaceException {
		while (!isStopRequested) {
			WorldOptions worldOptions = new WorldOptions(WorldSeed.random(), configuration.getWorldType());
			World world = runningLauncherProfile.createWorld(worldOptions);
			if (configuration.getWorldFilter().isValid(world)) {
				reporter.report(worldOptions);
				world.dispose();
				break;
			}
			world.dispose();
		}
	}
}
