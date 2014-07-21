package fr.openium.taskPool;

import java.util.Comparator;

public class TaskComparator<T extends AbstractMobileCrawler> implements Comparator<T> {

	@Override
	public int compare(AbstractMobileCrawler o1, AbstractMobileCrawler o2) {
		if (o1.getPriority() < o2.getPriority()) {
			return 1;
		}
		if (o1.getPriority() == o2.getPriority()) {
			return 0;
		}
		return -1;
	}

}
