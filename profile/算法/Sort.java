
public class Sort {

    //冒泡
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

    //希尔排序
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

    //merge two sorted order into one order, asc. 归并排序
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

    //快排
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

    //堆
    public static void heapify(int[] nums, int root, int length) {
        int leftLeaf = root * 2 + 1;    //左结点索引
        int rightLeaf = root * 2 + 2;   //右结点索引
        int largest = root;     //根节点索引，默认根最大
        if (leftLeaf < length && nums[leftLeaf] > nums[largest])
            largest = leftLeaf;     //左节点大，取左节点
        if (rightLeaf < length && nums[rightLeaf] > nums[largest])
            largest = rightLeaf;    //右节点大，取右节点
        if (largest != root) {
            swap(nums, root, largest);      //largest value down to bottom，根节点小，下沉至子结点
            heapify(nums, largest, length); //沿着子结点继续下沉
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

    //计数排序
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

    //桶排序
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

    //基数排序
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
}