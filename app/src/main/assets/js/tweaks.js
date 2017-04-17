// Resize iframes to the viewport size, preserving the aspect ratio if it seems to be a video
$(function()
{
	var $iframes = $("iframe");
	var $body = $("body");

	$iframes.each(function()
	{
		$(this).data('aspectRatio', this.height / this.width)
		.removeAttr('height')
		.removeAttr('width');
	});

	$(window).resize(function()
	{
		var newWidth = $body.width();

		$iframes.each(function() {
			var $iframe = $(this);

			if (($iframe.data('aspectRatio')) >= 0.4) /* 21x9 (cinemascope) */
			{
				$iframe
                	.width(newWidth)
                	.height(newWidth * $iframe.data('aspectRatio'));
			}
			else
			{
				$iframe.width(newWidth);
			}
		});
	}).resize();
});