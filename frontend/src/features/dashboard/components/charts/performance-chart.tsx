import { useCallback, useEffect, useMemo, useRef, useState, type PointerEvent } from 'react'
import { Box, useBreakpointValue, useTheme } from '@chakra-ui/react'
import * as echarts from 'echarts'
import { EChartsWrapper } from './echarts-wrapper'
import { getBodyText7Style } from './chart-text-style'
import type { EntityPerformance } from '../../types'

interface AllStatesPerformanceChartProps {
  data: EntityPerformance[]
  className?: string
  height?: string | number
  maxItems?: number
  entityLabel?: string
}

export function AllStatesPerformanceChart({
  data,
  className,
  height = '536px',
  maxItems = 5,
  entityLabel = 'States/UTs',
}: AllStatesPerformanceChartProps) {
  const theme = useTheme()
  const bodyText7 = getBodyText7Style(theme)
  const isCompact = useBreakpointValue({ base: true, md: false }) ?? false
  const normalizedMaxItems =
    typeof maxItems === 'number' && Number.isFinite(maxItems) ? Math.max(1, maxItems) : 1
  const chartScrollRef = useRef<HTMLDivElement>(null)
  const scrollbarTrackRef = useRef<HTMLDivElement>(null)
  const scrollbarThumbRef = useRef<HTMLDivElement>(null)
  const isDraggingThumb = useRef(false)
  const dragStartX = useRef(0)
  const dragStartLeft = useRef(0)
  const thumbLeftRef = useRef(0)
  const [containerWidth, setContainerWidth] = useState(0)

  const defaultItemWidth = isCompact ? 68 : 80
  const minItemWidth = isCompact ? 44 : 56
  const effectiveItemWidth =
    containerWidth > 0
      ? Math.max(minItemWidth, Math.floor(containerWidth / Math.max(data.length, 1)))
      : defaultItemWidth
  const itemWidth = Math.min(defaultItemWidth, effectiveItemWidth)
  const barWidth = Math.min(
    isCompact ? 28 : 34,
    Math.max(isCompact ? 12 : 16, Math.floor(itemWidth * 0.45))
  )
  const axisLabelFontSize = isCompact ? Math.max(10, bodyText7.fontSize - 2) : bodyText7.fontSize
  const axisLabelLineHeight = isCompact
    ? Math.max(12, bodyText7.lineHeight - 2)
    : bodyText7.lineHeight
  const axisLabelMargin = isCompact ? 10 : 15
  const axisLabelRotate = isCompact ? 60 : 45
  const axisWidth = isCompact ? '56px' : '72px'
  const axisLabelOffset = isCompact ? '-32px' : '-40px'
  const longestEntityLabel = useMemo(() => {
    return data.reduce((longest, item) => {
      return item.name.length > longest.length ? item.name : longest
    }, '')
  }, [data])

  const option = useMemo<echarts.EChartsOption>(() => {
    const sortedData = [...data].sort((a, b) => b.quantity - a.quantity)
    const entities = sortedData.map((d) => d.name)
    const quantity = sortedData.map((d) => d.quantity)
    const regularity = sortedData.map((d) => d.regularity)

    return {
      tooltip: {
        show: false,
      },
      grid: {
        left: isCompact ? 12 : 24,
        right: '4%',
        top: '10%',
        bottom: '2%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: true,
        data: entities,
        axisLine: {
          lineStyle: {
            color: '#E4E4E7',
          },
        },
        axisTick: {
          show: false,
        },
        axisLabel: {
          showMaxLabel: true,
          showMinLabel: true,
          rotate: axisLabelRotate,
          interval: 0,
          margin: axisLabelMargin,
          fontSize: axisLabelFontSize,
          lineHeight: axisLabelLineHeight,
          fontWeight: 400,
          color: bodyText7.color,
        },
      },
      yAxis: {
        type: 'value',
        name: '',
        axisLabel: {
          show: false,
        },
        axisLine: {
          show: false,
        },
        axisTick: {
          show: false,
        },
        max: 100,
        interval: 25,
        splitLine: {
          lineStyle: {
            color: '#E4E4E7',
          },
        },
      },
      series: [
        {
          name: 'Quantity',
          type: 'bar',
          data: quantity,
          barWidth,
          itemStyle: {
            color: '#3291D1',
            borderRadius: [4, 4, 0, 0],
          },
        },
        {
          name: 'Regularity',
          type: 'bar',
          data: regularity,
          barWidth,
          barGap: '30%',
          itemStyle: {
            color: '#ADD3ED',
            borderRadius: [4, 4, 0, 0],
          },
        },
      ],
    }
  }, [
    axisLabelFontSize,
    axisLabelLineHeight,
    axisLabelMargin,
    axisLabelRotate,
    barWidth,
    bodyText7,
    data,
    isCompact,
  ])

  const axisOption = useMemo<echarts.EChartsOption>(() => {
    const placeholderLabel = longestEntityLabel || 'W'
    return {
      tooltip: {
        show: false,
      },
      grid: {
        left: 0,
        right: 0,
        top: '10%',
        bottom: '2%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: [placeholderLabel],
        axisTick: {
          show: false,
        },
        axisLine: {
          show: false,
        },
        axisLabel: {
          show: true,
          rotate: axisLabelRotate,
          margin: axisLabelMargin,
          fontSize: axisLabelFontSize,
          lineHeight: axisLabelLineHeight,
          fontWeight: 400,
          formatter: (value: string) => value,
          color: 'transparent',
        },
      },
      yAxis: {
        type: 'value',
        position: 'right',
        axisLabel: {
          align: 'right',
          margin: 2,
          fontSize: axisLabelFontSize,
          lineHeight: axisLabelLineHeight,
          fontWeight: 400,
          color: bodyText7.color,
        },
        min: 0,
        max: 100,
        interval: 25,
        splitLine: {
          show: false,
        },
      },
      series: [
        {
          type: 'bar',
          data: [0],
          barWidth: 0,
          itemStyle: {
            opacity: 0,
          },
        },
      ],
      animation: false,
    }
  }, [
    axisLabelFontSize,
    axisLabelLineHeight,
    axisLabelMargin,
    axisLabelRotate,
    bodyText7,
    longestEntityLabel,
  ])

  const containerHeight = typeof height === 'number' ? `${height}px` : height
  const legendItems = [
    { label: 'Quantity', color: '#3291D1' },
    { label: 'Regularity', color: '#ADD3ED' },
  ]
  const baseChartWidth = data.length * itemWidth
  const chartPixelWidth =
    containerWidth > 0 ? Math.max(baseChartWidth, containerWidth) : baseChartWidth
  const shouldScroll =
    data.length > normalizedMaxItems && containerWidth > 0 && baseChartWidth > containerWidth
  const chartWidth = shouldScroll ? `${chartPixelWidth}px` : '100%'

  const getTrackWidth = () => {
    return scrollbarTrackRef.current?.getBoundingClientRect().width ?? 0
  }

  const updateThumbFromScroll = useCallback(() => {
    const node = chartScrollRef.current
    const thumb = scrollbarThumbRef.current
    if (!node || !thumb) return
    const trackWidth = getTrackWidth()
    if (trackWidth === 0) return
    const thumbWidth = Math.min(163, trackWidth)
    const maxThumbTravel = Math.max(0, trackWidth - thumbWidth)
    const maxScroll = node.scrollWidth - node.clientWidth
    const nextLeft =
      maxScroll <= 0 || maxThumbTravel === 0 ? 0 : (node.scrollLeft / maxScroll) * maxThumbTravel
    thumb.style.width = `${thumbWidth}px`
    thumb.style.left = `${nextLeft}px`
    thumbLeftRef.current = nextLeft
  }, [])

  useEffect(() => {
    const node = chartScrollRef.current
    if (!node) return

    const resizeObserver = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (entry) setContainerWidth(entry.contentRect.width)
      updateThumbFromScroll()
    })

    resizeObserver.observe(node)
    return () => {
      resizeObserver.disconnect()
    }
  }, [updateThumbFromScroll])

  useEffect(() => {
    const node = scrollbarTrackRef.current
    if (!node) return

    const resizeObserver = new ResizeObserver(() => {
      updateThumbFromScroll()
    })

    resizeObserver.observe(node)
    return () => {
      resizeObserver.disconnect()
    }
  }, [updateThumbFromScroll])

  const handleThumbPointerDown = (event: PointerEvent<HTMLDivElement>) => {
    if (!shouldScroll) return
    isDraggingThumb.current = true
    dragStartX.current = event.clientX
    dragStartLeft.current = thumbLeftRef.current
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  const handleThumbPointerMove = (event: PointerEvent<HTMLDivElement>) => {
    if (!isDraggingThumb.current) return
    const node = chartScrollRef.current
    if (!node) return
    const trackWidth = getTrackWidth()
    if (trackWidth === 0) return
    const thumbWidth = Math.min(163, trackWidth)
    const maxThumbTravel = Math.max(0, trackWidth - thumbWidth)
    if (maxThumbTravel === 0) return
    const delta = event.clientX - dragStartX.current
    const nextLeft = Math.min(Math.max(dragStartLeft.current + delta, 0), maxThumbTravel)
    const thumb = scrollbarThumbRef.current
    if (thumb) {
      thumb.style.left = `${nextLeft}px`
    }
    thumbLeftRef.current = nextLeft
    const maxScroll = node.scrollWidth - node.clientWidth
    node.scrollLeft = (nextLeft / maxThumbTravel) * maxScroll
  }

  const handleThumbPointerUp = (event: PointerEvent<HTMLDivElement>) => {
    if (!isDraggingThumb.current) return
    isDraggingThumb.current = false
    event.currentTarget.releasePointerCapture(event.pointerId)
  }

  useEffect(() => {
    updateThumbFromScroll()
  }, [data.length, containerWidth, updateThumbFromScroll])

  return (
    <div
      className={className}
      style={{
        width: '100%',
        minWidth: 0,
        height: containerHeight,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <Box flex={1} minH={0} minW={0} overflow="visible" display="flex">
        <Box width={axisWidth} flexShrink={0} position="relative">
          <EChartsWrapper option={axisOption} height="100%" />
          <Box
            position="absolute"
            left={axisLabelOffset}
            top="50%"
            transform="translateY(-50%) rotate(-90deg)"
            transformOrigin="center"
            textStyle="bodyText7"
            fontWeight="400"
            fontSize={axisLabelFontSize}
            lineHeight={`${axisLabelLineHeight}px`}
            color={bodyText7.color}
            whiteSpace="nowrap"
          >
            Quantity & Regularity
          </Box>
        </Box>
        <Box
          ref={chartScrollRef}
          overflowX={shouldScroll ? 'auto' : 'hidden'}
          overflowY="hidden"
          height="100%"
          flex="1"
          minW={0}
          onScroll={updateThumbFromScroll}
          sx={{
            scrollbarWidth: 'none',
            '&::-webkit-scrollbar': { height: '0px' },
          }}
        >
          <div
            style={{
              width: chartWidth,
              height: '100%',
            }}
          >
            <EChartsWrapper option={option} height="100%" />
          </div>
        </Box>
      </Box>
      <Box
        textAlign="center"
        textStyle="bodyText7"
        fontWeight="400"
        color={bodyText7.color}
        mt="4px"
      >
        {entityLabel}
      </Box>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '16px',
          flexWrap: 'wrap',
          rowGap: '6px',
          paddingTop: '8px',
        }}
      >
        {legendItems.map((item) => (
          <div key={item.label} style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span
              aria-hidden="true"
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '2px',
                backgroundColor: item.color,
                display: 'inline-block',
              }}
            />
            <span
              style={{
                fontSize: bodyText7.fontSize,
                lineHeight: `${bodyText7.lineHeight}px`,
                fontWeight: 400,
                color: bodyText7.color,
              }}
            >
              {item.label}
            </span>
          </div>
        ))}
      </div>
      <Box mt="6px">
        <Box
          ref={scrollbarTrackRef}
          height="4px"
          bg="neutral.200"
          borderRadius="999px"
          position="relative"
        >
          <Box
            role="presentation"
            position="absolute"
            top={0}
            height="4px"
            width="163px"
            maxW="100%"
            bg="neutral.300"
            borderRadius="999px"
            cursor={shouldScroll ? 'grab' : 'default'}
            ref={scrollbarThumbRef}
            onPointerDown={handleThumbPointerDown}
            onPointerMove={handleThumbPointerMove}
            onPointerUp={handleThumbPointerUp}
            onPointerLeave={handleThumbPointerUp}
          />
        </Box>
      </Box>
    </div>
  )
}
