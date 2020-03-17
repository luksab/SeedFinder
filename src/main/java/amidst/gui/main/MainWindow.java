package amidst.gui.main;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.JFrame;

import amidst.AmidstMetaData;
import amidst.FeatureToggles;
import amidst.documentation.AmidstThread;
import amidst.documentation.CalledOnlyBy;
import amidst.documentation.NotThreadSafe;
import amidst.gui.main.menu.AmidstMenu;
import amidst.gui.main.viewer.ViewerFacade;
import amidst.gui.seedsearcher.SeedSearcherWindow;
import amidst.logging.AmidstLogger;
import amidst.mojangapi.world.WorldOptions;
import amidst.mojangapi.world.WorldSeed;
import amidst.mojangapi.world.WorldType;

@NotThreadSafe
public class MainWindow {
	private final JFrame frame;
	private final WorldSwitcher worldSwitcher;
	private final Supplier<ViewerFacade> viewerFacadeSupplier;
	private final SeedSearcherWindow seedSearcherWindow;

	@CalledOnlyBy(AmidstThread.EDT)
	public MainWindow(JFrame frame, WorldSwitcher worldSwitcher, Supplier<ViewerFacade> viewerFacadeSupplier,
			SeedSearcherWindow seedSearcherWindow) {
		this.frame = frame;
		this.worldSwitcher = worldSwitcher;
		this.viewerFacadeSupplier = viewerFacadeSupplier;
		this.seedSearcherWindow = seedSearcherWindow;
	}

	@CalledOnlyBy(AmidstThread.EDT)
	public void initializeFrame(AmidstMetaData metadata, String versionString, Actions actions, AmidstMenu menuBar,
				 Optional<WorldOptions> initialWorldOptions) {
		frame.setSize(1000, 800);
		frame.setIconImages(metadata.getIcons());
		//frame.setTitle(versionString);
		frame.setTitle("UwU");
		System.out.println();
		frame.setJMenuBar(menuBar.getMenuBar());
		frame.getContentPane().setLayout(new BorderLayout());
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				actions.exit();
			}
		});
		frame.setVisible(true);
		worldSwitcher.clearWorld();
		WorldOptions worldOptions = new WorldOptions(WorldSeed.random(), WorldType.from("Default"));
		worldSwitcher.displayWorld(worldOptions);
		initialWorldOptions.ifPresent(options -> {
            AmidstLogger.info("Setting initial world options to [" + options.getWorldSeed().getLabel() + ", World Type: " + options.getWorldType() + "]");
            worldSwitcher.displayWorld(options);
		});
	}

	public WorldSwitcher getWorldSwitcher() {
		return worldSwitcher;
	}

	public ViewerFacade getViewerFacade() {
		return viewerFacadeSupplier.get();
	}

	@CalledOnlyBy(AmidstThread.EDT)
	public void dispose() {
		worldSwitcher.clearWorld();
		if (FeatureToggles.SEED_SEARCH) {
			seedSearcherWindow.dispose();
		}
		frame.dispose();
	}
}
