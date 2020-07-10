
import java.util.ArrayList;

/**
 * 二分查找，及其一些变体
 */
public class BinarySearch {

    /**
     * find the first position of needle in text, null return -1
     */
    public static int findStrPosition(String text, String needle) {
        if (text == null || text.length() == 0)
            return -1;
        if (needle == null || needle.length() == 0)
            return -1;
        char[] chars = text.toCharArray();
        char[] str = needle.toCharArray();
        for (int i = 0; i <= chars.length - str.length; i++) {
            if (str[0] == chars[i]) {
                String tmp = text.substring(i, needle.length() + i);
                if (needle.equals(tmp))
                    return i + 1;
            }
        }
        return -1;
    }

    public static int findStrPosition1(String text, String needle) {
        if (text == null || text.length() == 0)
            return -1;
        if (needle == null || needle.length() == 0)
            return -1;
        for (int i = 0; i <= text.length() - needle.length(); i++) {
            int j = 0;
            for (; j < needle.length(); j++) {
                if (needle.charAt(j) != text.charAt(i + j))
                    break;
            }
            if (j == needle.length())
                return i + 1;
        }
        return -1;
    }

    /**
     * backtrack
     */
    public static ArrayList<int[]> subSet(int[] nums) {
        ArrayList<int[]> children = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            children.add(new int[]{i});
        }
        return children;
    }

    /**
     * binary search, a ordered array
     */
    public static int search(int[] nums, int target) {
        int start = 0, end = nums.length - 1;
        while (start + 1 < end) {
            int mid = start + (end - start) / 2;
            if (nums[mid] == target)
                end = mid;
            else if (nums[mid] > target) {
                end = mid;
            } else {
                start = mid;
            }
        }
        if (nums[start] == target)
            return start;
        if (nums[end] == target)
            return end;
        return -1;
    }

    public static int searchFirst(int[] nums, int target) {
        int start = 0, end = nums.length - 1;
        while (start < end) {
            int mid = start + (end - start) / 2;
            if (nums[mid] < target)
                start = mid + 1;
            else {
                end = mid;
            }
        }
        if (nums[start] == target)
            return start;
        if (nums[end] == target)
            return end;
        return -1;
    }

    public static int searchLast(int[] nums, int target) {
        int start = 0, end = nums.length - 1;
        while (start + 1 < end) {
            int mid = start + (end - start) / 2;
            if (nums[mid] > target)
                end = mid;
            else if (nums[mid] < target) {
                start = mid;
            } else {
                start = mid;
            }
        }
        if (nums[end] == target)
            return end;
        if (nums[start] == target)
            return start;
        return -1;
    }

    /**
     * return insert position if not exist
     */
    public static int searchInsert(int[] nums, int target) {
        int start = 0, end = nums.length - 1;
        while (start < end) {
            int mid = start + (end - start) / 2;
            if (nums[mid] < target)
                start = mid + 1;
            else
                end = mid;
        }
        if (nums[start] >= target)
            return start;
        if (nums[end] >= target)
            return end;
        else if (nums[end] < target)
            return end + 1;
        return 0;
    }

    //search a target is exist in a matrix, the matrix is order asc
    public static boolean searchMatrix(int[][] nums, int target) {
        if (nums.length == 0 || nums[0].length == 0)
            return false;
        int row = nums.length;
        int col = nums[0].length;
        int start = 0, end = row * col - 1;
        while (start < end) {
            int mid = start + (end - start) / 2;
            if (nums[mid / (col)][mid % (col)] < target)
                start = mid + 1;
            else
                end = mid;
        }
        if (nums[start / (col)][start % (col)] == target)
            return true;
        if (nums[end / (col)][end % (col)] == target)
            return true;
        return false;
    }

    public static int findMin(int[] nums) {
        if (nums.length == 0) return -1;
        int start = 0, end = nums.length - 1;
        while (start + 1 < end) {
            while (start < end && nums[end] == nums[end - 1])
                end--;
            while (start < end && nums[start] == nums[start + 1])
                start++;
            int mid = start + (end - start) / 2;
            if (nums[mid] <= nums[end])
                end = mid;
            else
                start = mid;
        }
        if (nums[start] > nums[end])
            return nums[end];
        return nums[start];
    }

    // [0,1,2,4,5,6,7] change to [4,5,6,7,0,1,2], find a value index
    public static int findIndex(int[] nums, int target) {
        if (nums.length == 0) return -1;
        int start = 0, end = nums.length - 1;
        while (start + 1 < end) {
            while (start < end && nums[end] == nums[end - 1])
                end--;
            while (start < end && nums[start] == nums[start + 1])
                start++;
            int mid = start + (end - start) / 2;
            if (nums[mid] == target)
                return mid;
            if (nums[mid] > nums[start]) {
                if (nums[start] <= target && nums[mid] >= target)
                    end = mid;
                else
                    start = mid;
            } else if (nums[end] > nums[mid]) {
                if (nums[mid] <= target && nums[end] >= target)
                    start = mid;
                else
                    end = mid;
            }
        }
        if (nums[start] == target)
            return start;
        if (nums[end] == target)
            return end;
        return -1;
    }

    public static void main(String[] args) {
//        int position = findStrPosition1("java", "va");
        int position = findIndex(new int[]{5, 7, 8, 9, 11, 29, 33, 1, 3, 4, 5, 5}, 29);
        System.out.println("position: " + position);
    }
}