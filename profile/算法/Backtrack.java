
/**
 * 回溯法
 */
public class Backtrack {

    /**
     * 求一个序列的所有子集
     */
    public static ArrayList<ArrayList<Integer>> subSets(int[] nums) {
        ArrayList<Integer> tmpList = new ArrayList<>();
        ArrayList<ArrayList<Integer>> results = new ArrayList<>();
        Arrays.sort(nums);
        backtrack(nums, 0, tmpList, results);
        for (int i = 0; i < results.size(); i++) {
            System.out.println(Arrays.toString(results.get(i).toArray()));
        }
        return results;
    }

    /**
     * backtrack
     */
    public static void backtrack(int[] nums, int position, ArrayList<Integer> tmpList, ArrayList<ArrayList<Integer>> results) {
        results.add(new ArrayList<>(tmpList));
        if (tmpList.size() == nums.length)
            return;
        for (int i = position; i < nums.length; i++) {
            tmpList.add(nums[i]);
            backtrack(nums, i + 1, tmpList, results);
            tmpList.remove(tmpList.size() - 1);
        }
    }
}