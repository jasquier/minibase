package iterator;

import global.AttrType;
import global.GlobalConst;
import global.RID;
import global.SystemDefs;
import heap.FieldNumberOutOfBoundException;
import heap.Heapfile;
import heap.InvalidTupleSizeException;
import heap.InvalidTypeException;
import heap.Tuple;
import index.BloomFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Vector;

public class IEselfjoin2predicates {
	Tuple[] L1;
	Tuple[] L2;
	int n;
	int op1;
	int op2;

	private int[] p;
	private BitSet bp;
	public IEselfjoin2predicates(Tuple[] l1, Tuple[] l2, int n,int op1, int op2) {
		super();
		L1 = l1;
		L2 = l2;
		this.n = n;
		this.op1 = op1;
		this.op2 = op2;

		p = new int[l1.length];
		System.out.println("Lengath of p is "+p.length);
		bp = new BitSet(l1.length);
	}
	// [1] => id, [2] => duration, [3] => cost, [4] and above => others
	public ArrayList<Tuple[]> run() throws FieldNumberOutOfBoundException, IOException {
		// TODO: Decide what to do when exception is raised
		ArrayList<Tuple[]> join_result = new ArrayList<>();
		Comparator<Tuple> ascDuration = new Comparator<Tuple>() {
			@Override
			public int compare(Tuple o1, Tuple o2) {
				// sort on X
				int diff = 0;
				try {
					diff = o1.getIntFld(2) - o2.getIntFld(2);

				} catch (FieldNumberOutOfBoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (diff > 1)
					return 1;
				else if (diff == 0)
					return 0;
				else
					return -1;
			}
		};
		Comparator<Tuple> descDuration = new Comparator<Tuple>() {
			@Override
			public int compare(Tuple o1, Tuple o2) {
				// sort on X
				int diff = 0;
				try {
					diff = o1.getIntFld(2) - o2.getIntFld(2);
				} catch (FieldNumberOutOfBoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (diff > 1)
					return -1;
				else if (diff == 0)
					return 0;
				else
					return 1;
			}
		};
		Comparator<Tuple> ascCost = new Comparator<Tuple>() {
			@Override
			public int compare(Tuple o1, Tuple o2) {
				// sort on X
				int diff = 0;
				try {
					diff = o1.getIntFld(3) - o2.getIntFld(3);
				} catch (FieldNumberOutOfBoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (diff > 1)
					return 1;
				else if (diff == 0)
					return 0;
				else
					return -1;
			}
		};
		Comparator<Tuple> descCost = new Comparator<Tuple>() {
			@Override
			public int compare(Tuple o1, Tuple o2) {
				// sort on X
				int diff = 0;
				try {
					diff = o1.getIntFld(3) - o2.getIntFld(3);
				} catch (FieldNumberOutOfBoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (diff > 1)
					return -1;
				else if (diff == 0)
					return 0;
				else
					return 1;
			}
		};

		// Compute L1,L2
		if (op1 == 2 || op1 == 1) {
			Arrays.sort(L1, descDuration);
		} else if (op1 == 3 || op1 == 4) {
			Arrays.sort(L1, ascDuration);
		}

		if (op2 == 1 || op2 == 2) {
			Arrays.sort(L2, ascCost);
		} else if (op2 == 4 || op2 == 3) {
			Arrays.sort(L2, descCost);
		}

		// Compute permutation arrays - P
		int i = 0;
		for (Tuple myTuple : L2) {
			//System.out.println("My tuple is:"+Arrays.toString(myTuple.getTupleByteArray()));
			p[i++] = findId(L1, myTuple.getIntFld(1)); // Tuple implements Comparable// MyTuple implements Comparable
			//System.out.println("test");
			//System.out.println(p[i-1]);
		}	

		
		// intialize the bit array, set all to zero

		int eqOff = 0;
		if ((op1 == 2 || op1 == 3) && (op2 == 2 || op2 == 3))
			eqOff = 0;
		else
			eqOff = 1;

		// Visit
		//usingBitsetNaive(join_result, eqOff);
		// usingBitsetOptimized(join_result, eqOff);
		usingBloomFilter(join_result, eqOff, 2);
		return join_result;
	}
	private void usingBitsetNaive(ArrayList<Tuple[]> join_result, int eqOff) {
		for (int i = 0; i < n; i++) {
			int off2 = p[i];
			for (int j = 0; j <= Math.min(off2, n - 1); j++) {
				bp.set(off2); // = 1;
			}
			for (int k = off2 + eqOff; k < n; k++) { // TODO: check initialization
				if (bp.get(k)) {
					// add tuples w.r.t. (L2[i],L2p[k]) to join result
					join_result.add(new Tuple[] { L1[k], L1[p[i]] });
				}
			}
		}
	}
	// set the bloom filter pos
	// c = bp.getNext(pos + eqOff)
//	while(c >= 0) { 
//	k = Math.max(k, c);
//	int limit = Math.min(c + reduction_factor + 1, L1p.length);
//	while (k < limit) {
//		if (bp.get(k)) {
//			join_result.add(new Tuple[] { L2[i], L2p[k] });
//		}
//		k++;
//	}
//	c = b.nextSetChunk(k);
//}
//System.out.println(join_result.toString());

	private void usingBloomFilter(ArrayList<Tuple[]> join_result, int eqOff, int reduction_factor) {
		BloomFilter b = new BloomFilter(L1.length, reduction_factor);
		for (int i = 0; i < n; i++) {
			int off2 = p[i];
			for (int j = 0; j <= Math.min(off2, n - 1); j++) {
				bp.set(off2); // = 1;
				b.setBit(off2);
			}
			int k = off2 + eqOff;
			int c = b.nextSetChunk(k); // TODO: check initialization
			while (c >= 0) {
				// traverse the chunk
				k = Math.max(k, c);
				int limit = Math.min(c + reduction_factor + 1, L1.length);
				while (k < limit) {
					if (bp.get(k)) {
						join_result.add(new Tuple[] { L1[k], L1[p[i]] });
					}
					k++;
				}
				c = b.nextSetChunk(k);
			}
		}
	}

	private void usingBitsetOptimized(ArrayList<Tuple[]> join_result, int eqOff) {
		for (int i = 0; i < n; i++) {
			int off2 = p[i];
			for (int j = 0; j <= Math.min(off2, n - 1); j++) {
				bp.set(off2); // = 1;
			}
			int k = bp.nextSetBit(off2 + eqOff); // TODO: check initialization
			while (k >= 0) {
				// add tuples w.r.t. (L1[i],L1[k]) to join result
				join_result.add(new Tuple[] { L1[k], L1[p[i]] });
				k = bp.nextSetBit(k + 1);
			}
		}
	}

	private int findId(Tuple[] a, int id) throws FieldNumberOutOfBoundException, IOException {
		int i = 0;
		for (Tuple myTuple : a) {
			if (myTuple.getIntFld(1) == id)
				return i;
			i++;
		}
		return -1;
	}

	public static Tuple create(int id, int duration, int cost, AttrType[] Stypes)
			throws FieldNumberOutOfBoundException, IOException, InvalidTypeException, InvalidTupleSizeException {
		Tuple t = new Tuple();
		t.setHdr((short) 4, Stypes, null);
		t.setIntFld(1, id);
		t.setIntFld(2, duration);
		t.setIntFld(3, cost);
		return t;
	}

	public static String tupleToString(Tuple t) throws FieldNumberOutOfBoundException, IOException {
		return String.format("[%d, %d, %d]", t.getIntFld(1), t.getIntFld(2), t.getIntFld(3));
	}

	public static void main(String[] args)
			throws FieldNumberOutOfBoundException, IOException, InvalidTypeException, InvalidTupleSizeException {
		final int NUMBUF = 50;
		// String dbpath = "/tmp/" + System.getProperty("user.name") + ".minibase.jointestdb";
		// SystemDefs sysdef = new SystemDefs(dbpath, 1000, NUMBUF, "Clock");

		// Setting the types
		AttrType[] Stypes = new AttrType[4];
		Stypes[0] = new AttrType(AttrType.attrInteger);
		Stypes[1] = new AttrType(AttrType.attrInteger);
		Stypes[2] = new AttrType(AttrType.attrInteger);
		Stypes[3] = new AttrType(AttrType.attrInteger);

		// Table T
		Tuple t1 = create(404, 100, 6, Stypes);
		Tuple t2 = create(498, 140, 11, Stypes);
		Tuple t3 = create(676, 80, 10, Stypes);
		Tuple t4 = create(742, 90, 9, Stypes);

		// Operators Map: 1 for <, 2 for <=, 3 for >= and 4 for >
		IEselfjoin2predicates iejoin = new IEselfjoin2predicates(new Tuple[] { t1, t2, t3,t4 }, new Tuple[] { t1, t2, t3,t4 },4,1,4);
		// TODO: Incorrect answer for the below case
		// iejoin = new IEselfjoin2predicates(new Tuple[] { t1, t2, t3 }, new Tuple[] { t1, t2, t3 },
		// new Tuple[] { tp1, tp2, tp3, tp4 }, new Tuple[] { tp1, tp2, tp3, tp4 }, 3, 4, 4, 1);

		ArrayList<Tuple[]> result = null;
		try {
			result = iejoin.run();
		} catch (FieldNumberOutOfBoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (Tuple[] myTuples : result) {
			System.out.format("[%s, %s]\n", tupleToString(myTuples[0]), tupleToString(myTuples[1]));
		}

	}

}
