package amidst.gui.seedsearcher;

import java.util.Set;

import amidst.documentation.NotThreadSafe;
import amidst.mojangapi.world.World;
import amidst.mojangapi.world.biome.Biome;
import amidst.mojangapi.world.biome.UnknownBiomeIndexException;
import amidst.mojangapi.world.filter.WorldFilter;

@NotThreadSafe
public class MySeedFilter extends WorldFilter {
	private short[][] region;
	private int optimize = 10;

	public MySeedFilter(long worldFilterSize) {
		super(worldFilterSize);
		this.region = new short[(int) this.quarterFilterSize * 2][(int) this.quarterFilterSize * 2];
	}

	@Override
	public boolean isValid(World world) {
		world.getBiomeDataOracle().populateArray(corner, region, true);
		int[] biomCount = new int[256];
		for (short i = 0; i < biomCount.length; i++) {
			  biomCount[i] = 0;
			}
		for (short[] row : region) {
			for (short entry : row) {
				biomCount[entry]++;
			}
		}	
		try {
			System.out.println(Biome.getByIndex(16).getName());
		} catch (UnknownBiomeIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(biomCount[16]);
		return( biomCount[16] > 3000 && (biomCount[12]>2 || biomCount[13]>2) && biomCount[14]>0 && biomCount[21]>0 && biomCount[3]>0);
		/*if(biomCount[16] > optimize && (biomCount[12]>2 || biomCount[13]>2) && biomCount[14]>0) {
			//System.out.println("yee");
			optimize = biomCount[16];
			return true;
		}
		else
			return false;	*/
	}
}
