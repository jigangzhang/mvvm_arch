//package com.god.seep.base.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Practice {

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

    public static void bubbleSort(int[] nums) {     //O(n^2)
        if (nums.length <= 1)
            return;
        for (int i = nums.length - 1; i > 0; i--) {
            for (int j = 0; j < i; j++)
                if (nums[j] > nums[j + 1]) {
                    int tmp = nums[j + 1];
                    nums[j + 1] = nums[j];
                    nums[j] = tmp;
                }
        }
        System.out.println("result: " + Arrays.toString(nums));
    }

    public static void selectSort(int[] nums) {
        if (nums.length <= 1)
            return;
        for (int i = 0; i < nums.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[j] < nums[min])
                    min = j;
            }
            int tmp = nums[min];
            nums[min] = nums[i];
            nums[i] = tmp;
        }
        System.out.println("result: " + Arrays.toString(nums));
    }

    public static void insertionSort(int[] nums) {
        if (nums.length <= 1)
            return;
        for (int i = 1; i < nums.length; i++) {
            for (int j = i; j > 0; j--) {
                if (nums[j] < nums[j - 1]) {
                    int tmp = nums[j];
                    nums[j] = nums[j - 1];
                    nums[j - 1] = tmp;
                } else
                    break;
            }
        }
        System.out.println("result: " + Arrays.toString(nums));
    }

    public static void insertionSort1(int[] nums) {
        if (nums.length <= 1)
            return;
        for (int i = 1; i < nums.length; i++) {
            int preIndex = i - 1, current = nums[i];
            while (preIndex >= 0 && nums[preIndex] > current) {
                nums[preIndex + 1] = nums[preIndex];
                preIndex--;
            }
            nums[preIndex + 1] = current;
        }
        System.out.println("result: " + Arrays.toString(nums));
    }

    public static void shellSort(int[] nums) {
        if (nums.length <= 1)
            return;
        for (int gap = nums.length / 2; gap > 0; gap = gap / 2) {
            for (int i = gap; i < nums.length; i++) {
                for (int j = i; (j - gap) >= 0; j = j - gap) {
                    if (nums[j] < nums[j - gap]) {
                        int tmp = nums[j];
                        nums[j] = nums[j - gap];
                        nums[j - gap] = tmp;
                    } else
                        break;
                }
            }
        }
        System.out.println("result: " + Arrays.toString(nums));
    }

    //merge two sorted order into one order, asc
    public static int[] merge(int[] left, int[] right) {
        int l = 0, r = 0, t = 0;
        int[] tmp = new int[left.length + right.length];
        while (l < left.length && r < right.length) {
            if (left[l] > right[r])
                tmp[t++] = right[r++];
            else
                tmp[t++] = left[l++];
        }
        while (l < left.length)
            tmp[t++] = left[l++];
        while (r < right.length)
            tmp[t++] = right[r++];
        return tmp;
    }

    public static int[] mergeSort(int[] nums) {     //O(nlogn)
        if (nums.length <= 1)
            return nums;
        int mid = nums.length / 2;
        int[] left = new int[mid];
        int[] right = new int[nums.length - mid];
        for (int i = 0; i < mid; i++) {
            left[i] = nums[i];
        }
        for (int i = mid; i < nums.length; i++) {
            right[i - mid] = nums[i];
        }
        return merge(mergeSort(left), mergeSort(right));
    }

    public static int[] quickSort(int[] nums) {
        quickSort(nums, 0, nums.length - 1);
        return nums;
    }

    public static int[] quickSort(int[] nums, int left, int right) {
        if (left < right) {
            int fixIndex = partition(nums, left, right);
            quickSort(nums, left, fixIndex - 1);
            quickSort(nums, fixIndex + 1, right);
        }
        return nums;
    }

    public static int partition(int[] nums, int left, int right) {
        int pivot = left;
        int index = pivot + 1;
        for (int i = index; i <= right; i++) {
            if (nums[i] < nums[pivot]) {
                swap(nums, i, index);
                index++;
            }
        }
        swap(nums, pivot, index - 1);
        return index - 1;
    }

    public static void swap(int[] nums, int i, int j) {
        int tmp = nums[i];
        nums[i] = nums[j];
        nums[j] = tmp;
    }

    public static void heapify(int[] nums, int root, int length) {
        int leftLeaf = root * 2 + 1;
        int rightLeaf = root * 2 + 2;
        int largest = root;
        if (leftLeaf < length && nums[leftLeaf] > nums[largest])
            largest = leftLeaf;
        if (rightLeaf < length && nums[rightLeaf] > nums[largest])
            largest = rightLeaf;
        if (largest != root) {
            swap(nums, root, largest);      //largest value down to bottom
            heapify(nums, largest, length);
        }
    }

    public static void buildMaxHeap(int[] nums) {
        for (int i = nums.length / 2; i >= 0; i--)
            heapify(nums, i, nums.length);
    }

    public static int[] heapSort(int[] nums) {
        buildMaxHeap(nums);
        int length = nums.length;
        for (int i = nums.length - 1; i > 0; i--) {
            swap(nums, 0, i);
            heapify(nums, 0, --length);
        }
        return nums;
    }

    public static int[] countingSort(int[] nums) {
        int max = 0;
        for (int num : nums) {
            if (num > max)
                max = num;
        }
        int[] tmp = new int[max + 1];
        for (int i = 0; i < nums.length; i++) {
            int key = nums[i];
            int value = tmp[key] + 1;
            tmp[key] = value;
        }
        int k = 0;
        for (int i = 0; i < tmp.length; i++) {
            while (tmp[i] > 0) {
                nums[k++] = i;
                tmp[i]--;
            }
        }
        return nums;
    }

    public static int[] countingSort1(int[] nums) {
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int num : nums) {
            if (num > max)
                max = num;
            if (num < min)
                min = num;
        }
        int[] tmp = new int[max - min + 1];
        for (int i = 0; i < nums.length; i++) {
            int key = nums[i];
            int value = tmp[key - min] + 1;
            tmp[key] = value;
        }
        int k = 0;
        for (int i = min; i <= max; i++) {
            while (tmp[i - min] > 0) {
                nums[k++] = i;
                tmp[i - min]--;
            }
        }
        return nums;
    }

    public static int[] bucketSort(int[] nums) {
        if (nums.length <= 1)
            return nums;
        int max = nums[0];
        int min = nums[0];
        for (int i = 1; i < nums.length; i++) {
            if (max < nums[i])
                max = nums[i];
            else if (min > nums[i])
                min = nums[i];
        }
        int defaultBucketSize = 5;
        int bucketNum = ((max - min) / nums.length) + 1;
        ArrayList<ArrayList<Integer>> buckets = new ArrayList<>(bucketNum);
        for (int i = 0; i < bucketNum; i++)
            buckets.add(new ArrayList<Integer>());
        for (int i = 0; i < nums.length; i++) {
            int num = (nums[i] - min) / nums.length;
            buckets.get(num).add(nums[i]);
        }
        int k = 0;
        for (int i = 0; i < buckets.size(); i++) {
            Collections.sort(buckets.get(i));
            for (int j = 0; j < buckets.get(i).size(); j++) {
                nums[k++] = buckets.get(i).get(j);
            }
        }
        return nums;
    }

    public static int[] radixSort(int[] nums) {
        int max = nums[0];
        int min = nums[0];
        for (int i = 0; i < nums.length; i++) {
            if (max < nums[i])
                max = nums[i];
            else if (min > nums[i])
                min = nums[i];
        }
        int mod = 10;
        int maxBit = 1;
        while (max / mod > 0) {
            mod *= 10;
            maxBit += 1;
        }
        mod = 10;
        int dev = 1;
        for (int i = 0; i < maxBit; i++, dev *= 10, mod *= 10) {
            ArrayList<ArrayList<Integer>> buckets = new ArrayList<>(10);
            for (int b = 0; b < 10; b++)
                buckets.add(null);
            for (int j = 0; j < nums.length; j++) {
                int bucket = (nums[j] % mod) / dev;
                if (buckets.get(bucket) == null)
                    buckets.add(bucket, new ArrayList<>());
                buckets.get(bucket).add(nums[j]);
            }
            int k = 0;
            for (int j = 0; j < buckets.size(); j++) {
                if (buckets.get(j) != null)
                    for (int m = 0; m < buckets.get(j).size(); m++)
                        nums[k++] = buckets.get(j).get(m);
            }
        }
        return nums;
    }

    public static void main(String[] args) {
//        int position = findStrPosition1("java", "va");
        subSets(new int[]{33, 29, 11, /*9, 0, 34, 8, 7, 5, 5, 5, 4, 3,*/ 1});
//        System.out.println("position: " + Arrays.toString(sort));
    }
}