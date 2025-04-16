package com.triple.backend.batch.config;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RecBookBatchTimingListener implements StepExecutionListener, ChunkListener {

	private StopWatch stepStopWatch;
	private StopWatch chunkStopWatch;

	@Override
	public void beforeStep(StepExecution stepExecution) {
		stepStopWatch = new StopWatch();
		stepStopWatch.start();
		log.info("[STEP] {} 시작", stepExecution.getStepName());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		stepStopWatch.stop();
		log.info("[STEP] {} 종료. 총 소요 시간: {}ms", stepExecution.getStepName(), stepStopWatch.getTotalTimeMillis());
		return ExitStatus.COMPLETED;
	}

	@Override
	public void beforeChunk(ChunkContext context) {
		chunkStopWatch = new StopWatch();
		chunkStopWatch.start();
	}

	@Override
	public void afterChunk(ChunkContext context) {
		chunkStopWatch.stop();
		log.debug("[CHUNK] 처리 시간: {}ms", chunkStopWatch.getTotalTimeMillis());
	}
}
