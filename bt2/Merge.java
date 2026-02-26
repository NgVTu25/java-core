package bt2;

public class Merge implements Runnable {

    private int[] arr;
    private int left;
    private int middle;
    private int right;

    public Merge(int[] arr, int left, int middle, int right) {
        this.arr = arr;
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    @Override
    public void run() {
        MergeSort.merge(arr, left, middle, right);
    }
}