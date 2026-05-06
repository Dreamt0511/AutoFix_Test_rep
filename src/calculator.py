"""计算器模块 — 包含若干 bug"""
import logging

logger = logging.getLogger(__name__)


def divide(a, b):
    """除法 — bug: 未处理除零"""
    logger.info(f"divide({a}, {b})")
    try:
        return a / b
    except ZeroDivisionError:
        logger.exception(f"divide({a}, {b}) 除零异常")
        raise


def average(numbers):
    """求平均值 — bug: 空列表时崩溃"""
    logger.info(f"average({numbers})")
    if not numbers:
        logger.exception(f"average({numbers}) 空列表求平均异常")
        raise ValueError("求平均值的列表不能为空")
    total = sum(numbers)
    return total / len(numbers)


def discount(price, rate):
    """打折计算 — bug: rate 范围未校验，传入负数或 >1 时产生错误结果"""
    logger.info(f"discount({price}, {rate})")
    if rate < 0 or rate > 1:
        logger.error(f"discount rate 超出范围: rate={rate}")
        raise ValueError(f"折扣率必须在0到1之间，当前值：{rate}")
    return price * (1 - rate)


def sqrt_approx(x, guess=1.0, iterations=10):
    """牛顿法求平方根 — bug: 未处理负数输入"""
    logger.info(f"sqrt_approx({x}, guess={guess})")
    if x < 0:
        logger.error(f"sqrt_approx 输入不能为负数: x={x}")
        raise ValueError(f"求平方根的输入不能为负数，当前值：{x}")
    try:
        for i in range(iterations):
            guess = (guess + x / guess) / 2
        return guess
    except (ValueError, ZeroDivisionError):
        logger.exception(f"sqrt_approx({x}) 计算异常")
        raise
